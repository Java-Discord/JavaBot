package com.javadiscord.javabot.commands.jam;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamPhase;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The phase manager is responsible for the logic that is required to transition
 * to each phase of the Jam.
 */
@RequiredArgsConstructor
public class JamPhaseManager {
	private static final Logger log = LoggerFactory.getLogger(JamPhaseManager.class);
	private static final String[] REACTION_NUMBERS = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};

	private final JamDataManager dataManager;

	/**
	 * Moves the jam into the theme voting phase. This involves the following:
	 * <ol>
	 *     <li>Clearing all messages from the jam-voting channel.</li>
	 *     <li>Creating an embed showing all themes, and save the id of this message.</li>
	 *     <li>Add a numbered reaction for each theme so people can vote.</li>
	 *     <li>Create an announcement in the jam-announcement channel.</li>
	 * </ol>
	 * @param jam The jam to update.
	 * @param event The event that triggered this action.
	 */
	public void moveToThemeVoting(Jam jam, SlashCommandEvent event) throws SQLException, IOException {
		TextChannel votingChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamVotingChannelId"));
		TextChannel announcementChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamAnnouncementChannelId"));

		log.info("Removing all messages from voting channel.");
		List<Message> messages;
		do {
			messages = votingChannel.getHistory().retrievePast(50).complete();
			if (messages.isEmpty()) break;
			if (messages.size() == 1) {
				votingChannel.deleteMessageById(messages.get(0).getIdLong()).complete();
			} else {
				votingChannel.deleteMessages(messages).complete();
			}
		} while (!messages.isEmpty());

		log.info("Creating theme voting embed.");
		EmbedBuilder voteEmbedBuilder = new EmbedBuilder()
				.setTitle("Jam Theme Voting")
				.setDescription("Vote for your preferred themes for the upcoming Java Jam.");
		List<JamTheme> themes = this.dataManager.getThemes(jam);
		for (int i = 0; i < themes.size(); i++) {
			JamTheme theme = themes.get(i);
			voteEmbedBuilder.addField(REACTION_NUMBERS[i] + " " + theme.getName(), theme.getDescription(), false);
		}

		log.info("Adding reactions to voting embed message.");
		Message themeVoteMessage = votingChannel.sendMessageEmbeds(voteEmbedBuilder.build()).complete();
		for (int i = 0; i < themes.size(); i++) {
			themeVoteMessage.addReaction(REACTION_NUMBERS[i]).complete();
		}
		this.dataManager.saveMessageId(jam, themeVoteMessage.getIdLong(), "theme_voting");

		log.info("Creating announcement.");
		announcementChannel.sendMessage("The Jam has entered theme voting! Vote for your theme now!!!").complete();

		log.info("Updating Jam status.");
		this.dataManager.updateJamPhase(jam, JamPhase.THEME_VOTING);
	}

	/**
	 * Moves the jam into the submission phase. This involves the following:
	 * <ol>
	 *     <li>Counting and recording all votes for all themes.</li>
	 *     <li>Removing the theme voting message.</li>
	 *     <li>Marking every theme in the Jam as either accepted or not accepted.</li>
	 *     <li>Begin allowing submissions via /submit, for only the accepted themes.</li>
	 *     <li>Create an announcement in the jam-announcement channel.</li>
	 * </ol>
	 * @param jam The jam to update.
	 * @param event The event that triggered this action.
	 */
	public void moveToSubmission(Jam jam, SlashCommandEvent event) throws SQLException, IOException {
		TextChannel votingChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamVotingChannelId"));
		TextChannel announcementChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamAnnouncementChannelId"));

		log.info("Counting votes for jam.");
		Message themeVotingMessage = votingChannel.getHistory().getMessageById(this.dataManager.getMessageId(jam, "theme_voting"));
		List<JamTheme> themes = this.dataManager.getThemes(jam);

		Connection con = Bot.dataSource.getConnection();
		con.setAutoCommit(false);

		Map<JamTheme, Integer> votes = this.recordThemeVotes(jam, themes, themeVotingMessage, con);
		List<JamTheme> rankedThemes = votes.entrySet().stream()
				.sorted(Comparator.comparingInt(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		// TODO: Accept winning theme, reject others, make announcement, and move jam to Submission.
	}

	private Map<JamTheme, Integer> recordThemeVotes(Jam jam, List<JamTheme> themes, Message themeVotingMessage, Connection con) throws SQLException {
		Map<JamTheme, Integer> votes = new HashMap<>();
		themes.forEach(theme -> votes.put(theme, 0));
		PreparedStatement themeVoteStmt = con.prepareStatement("INSERT INTO jam_theme_vote (jam_id, user_id, theme_name) VALUES (?, ?, ?)");
		themeVoteStmt.setLong(1, jam.getId());
		for (int i = 0; i < themes.size(); i++) {
			JamTheme theme = themes.get(i);
			String reactionUnicode = REACTION_NUMBERS[i];
			List<User> users = themeVotingMessage.retrieveReactionUsers(reactionUnicode).complete();
			for (User user : users) {
				votes.put(theme, votes.get(theme) + 1);
				themeVoteStmt.setLong(2, user.getIdLong());
				themeVoteStmt.setString(3, theme.getName());
				int rows = themeVoteStmt.executeUpdate();
				if (rows != 1) {
					log.error("Could not record vote of " + user.getAsTag() + " for theme " + theme.getName());
				}
			}
		}
		themeVoteStmt.close();
		return votes;
	}
}
