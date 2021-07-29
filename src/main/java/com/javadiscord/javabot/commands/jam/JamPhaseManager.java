package com.javadiscord.javabot.commands.jam;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.jam.dao.JamMessageRepository;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.commands.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamPhase;
import com.javadiscord.javabot.commands.jam.model.JamSubmission;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import com.javadiscord.javabot.other.Colors;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The phase manager is responsible for the logic that is required to transition
 * to each phase of the Jam.
 */
@RequiredArgsConstructor
public class JamPhaseManager {
	private static final Logger log = LoggerFactory.getLogger(JamPhaseManager.class);
	private static final String[] REACTION_NUMBERS = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	private static final String SUBMISSION_VOTE_UNICODE = "⬆";

	private final Connection con;

	/**
	 * Moves the jam to the next phase, or completes the jam if it's at the end
	 * of its life cycle.
	 * @param jam The jam to update.
	 * @param event The event which triggered this action.
	 * @throws Exception If an error occurs during updating.
	 */
	public void nextPhase(Jam jam, SlashCommandEvent event) throws Exception {
		switch (jam.getCurrentPhase()) {
			case JamPhase.THEME_PLANNING:
				this.moveToThemeVoting(jam, event);
				break;
			case JamPhase.THEME_VOTING:
				this.moveToSubmission(jam, event);
				break;
			case JamPhase.SUBMISSION:
				this.moveToSubmissionVoting(jam, event);
				break;
			case JamPhase.SUBMISSION_VOTING:
				this.completeJam(jam, event);
				break;
		}
	}

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
		if (votingChannel == null) throw new IllegalArgumentException("Invalid jam voting channel id.");
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");

		log.info("Removing all messages from voting channel.");
		this.removeAllMessages(votingChannel);
		log.info("Creating theme voting embed.");
		EmbedBuilder voteEmbedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s Theme Voting", jam.getFullName()))
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription("Vote for your preferred themes for the upcoming Jam.");
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(jam);
		if (themes.isEmpty()) throw new IllegalStateException("Cannot start theme voting until at least one theme is available.");
		for (int i = 0; i < themes.size(); i++) {
			JamTheme theme = themes.get(i);
			voteEmbedBuilder.addField(REACTION_NUMBERS[i] + " " + theme.getName(), theme.getDescription(), false);
		}
		log.info("Adding reactions to voting embed message.");
		Message themeVoteMessage = votingChannel.sendMessageEmbeds(voteEmbedBuilder.build()).complete();
		for (int i = 0; i < themes.size(); i++) {
			themeVoteMessage.addReaction(REACTION_NUMBERS[i]).complete();
		}
		new JamMessageRepository(con).saveMessageId(jam, themeVoteMessage.getIdLong(), "theme_voting");
		log.info("Creating announcement.");
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s Theme Voting Has Started!", jam.getFullName()))
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription("Go to " + votingChannel.getAsMention() + " to cast your votes, and decide what theme will be chosen for the Jam!");
		announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole(announcementChannel, event);
		log.info("Updating Jam status.");
		new JamRepository(con).updateJamPhase(jam, JamPhase.THEME_VOTING);
	}

	/**
	 * Moves the jam into the submission phase. This involves the following:
	 * <ol>
	 *     <li>Counting and recording all votes for all themes.</li>
	 *     <li>Removing the theme voting message.</li>
	 *     <li>Marking every theme in the Jam as either accepted or not accepted.</li>
	 *     <li>Begin allowing submissions via /jam submit, for only the accepted themes.</li>
	 *     <li>Create an announcement in the jam-announcement channel.</li>
	 * </ol>
	 * @param jam The jam to update.
	 * @param event The event that triggered this action.
	 */
	public void moveToSubmission(Jam jam, SlashCommandEvent event) throws SQLException, IOException {
		TextChannel votingChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamVotingChannelId"));
		TextChannel announcementChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamAnnouncementChannelId"));
		if (votingChannel == null) throw new IllegalArgumentException("Invalid jam voting channel id.");
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");

		JamMessageRepository messageRepository = new JamMessageRepository(con);

		log.info("Counting votes for jam.");
		Message themeVotingMessage = votingChannel.retrieveMessageById(messageRepository.getMessageId(jam, "theme_voting")).complete();
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(jam);

		Map<JamTheme, Integer> votes = this.recordThemeVotes(jam, themes, themeVotingMessage);
		JamTheme winningTheme = null;
		int winningThemeVotes = 0;
		for (Map.Entry<JamTheme, Integer> entry : votes.entrySet()) {
			log.info("Theme {} got {} votes.", entry.getKey().getName(), entry.getValue());
			if (entry.getValue() > winningThemeVotes) {
				winningTheme = entry.getKey();
				winningThemeVotes = entry.getValue();
			}
		}

		if (winningTheme == null) {
			log.warn("No winning jam theme could be found.");
			throw new IllegalStateException("No winning jam theme could be found.");
		}

		this.updateThemeAccepted(themes, winningTheme);
		this.sendChosenThemeMessage(announcementChannel, votes, winningTheme);
		this.pingRole(announcementChannel, event);

		log.info("Moving jam to submission phase.");
		new JamRepository(con).updateJamPhase(jam, JamPhase.SUBMISSION);
		log.info("Removing all messages from voting channel.");
		this.removeAllMessages(votingChannel);
		messageRepository.removeMessageId(jam, "theme_voting");
	}

	/**
	 * Moves the jam into the submission voting phase, where users can vote on
	 * the submissions to the jam. This does the following:
	 * <ol>
	 *     <li>Clears the voting channel and makes a single embed message for each
	 *     submission, with a single reaction below for users to vote on it.</li>
	 *     <li>Stop allowing submissions via /jam submit.</li>
	 * </ol>
	 * @param jam The jam to update.
	 * @param event The event that triggered this action.
	 */
	public void moveToSubmissionVoting(Jam jam, SlashCommandEvent event) throws SQLException, IOException {
		TextChannel votingChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamVotingChannelId"));
		TextChannel announcementChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamAnnouncementChannelId"));
		if (votingChannel == null) throw new IllegalArgumentException("Invalid jam voting channel id.");
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");

		log.info("Clearing voting channel.");
		this.removeAllMessages(votingChannel);
		List<JamSubmission> submissions = new JamSubmissionRepository(con).getSubmissions(jam);
		if (submissions.isEmpty()) {
			throw new IllegalStateException("Cannot start submission voting because there aren't any submissions.");
		}
		JamMessageRepository messageRepository = new JamMessageRepository(con);
		for (JamSubmission submission : submissions) {
			User user = event.getJDA().getUserById(submission.getUserId());
			String userName = user == null ? "Unknown user" : user.getAsTag();
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Submission by " + userName, submission.getSourceLink())
					.setColor(Colors.randomPastel())
					.setTimestamp(submission.getCreatedAt())
					.addField("Description", submission.getDescription(), false);
			log.info("Generating message in voting channel for submission {}.", submission.getId());
			Message message = votingChannel.sendMessageEmbeds(embedBuilder.build()).complete();
			message.addReaction(SUBMISSION_VOTE_UNICODE).complete();
			messageRepository.saveMessageId(jam, message.getIdLong(), "submission-" + submission.getId());
		}

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Voting Has Begun!")
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription(String.format("Go to %s to vote for who you think should win the %s.", votingChannel.getAsMention(), jam.getFullName()));
		announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole(announcementChannel, event);
		log.info("Setting Jam to Submission Voting phase.");
		new JamRepository(con).updateJamPhase(jam, JamPhase.SUBMISSION_VOTING);
	}

	/**
	 * Completes the Jam, by doing the following:
	 * <ol>
	 *     <li>Counting the number of votes each submission received.</li>
	 *     <li>Announcing a winner for the Jam.</li>
	 *     <li>Marking the Jam as complete, and setting its phase to NULL.</li>
	 *     <li>Clearing the relevant channels in preparation for the next jam.</li>
	 * </ol>
	 * @param jam The jam to update.
	 * @param event The event that triggered this action.
	 */
	public void completeJam(Jam jam, SlashCommandEvent event) throws SQLException, IOException {
		TextChannel votingChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamVotingChannelId"));
		TextChannel announcementChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamAnnouncementChannelId"));
		if (votingChannel == null) throw new IllegalArgumentException("Invalid jam voting channel id.");
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");

		JamMessageRepository messageRepository = new JamMessageRepository(con);
		List<JamSubmission> submissions = new JamSubmissionRepository(con).getSubmissions(jam);

		JamSubmission winningSubmission = null;
		int winningSubmissionVotes = -1;

		PreparedStatement submissionVoteStmt = con.prepareStatement("INSERT INTO jam_submission_vote (submission_id, user_id) VALUES (?, ?)");

		Map<JamSubmission, Integer> votesMap = new HashMap<>();
		for (JamSubmission submission : submissions) {
			submissionVoteStmt.setLong(1, submission.getId());
			Long messageId = messageRepository.getMessageId(jam, "submission-" + submission.getId());
			if (messageId == null) throw new IllegalStateException("Could not find message id for submission-" + submission.getId());
			Message message = votingChannel.retrieveMessageById(messageId).complete();
			List<User> users = message.retrieveReactionUsers(SUBMISSION_VOTE_UNICODE).complete();
			int votes = 0;
			for (User user : users) {
				if (user.isBot()) continue;
				submissionVoteStmt.setLong(2, user.getIdLong());
				int rows = submissionVoteStmt.executeUpdate();
				if (rows != 1) {
					log.error("Could not record vote of {} for submission {}.", user.getAsTag(), submission.getId());
				}
				votes++;
			}
			votesMap.put(submission, votes);
			if (votes > winningSubmissionVotes) {
				winningSubmission = submission;
				winningSubmissionVotes = votes;
			}
			messageRepository.removeMessageId(jam, "submission-" + submission.getId());
		}

		if (winningSubmission == null) {
			announcementChannel.sendMessage("No winning submission could be determined.").complete();
		} else {
			User winner = event.getJDA().getUserById(winningSubmission.getUserId());
			Guild guild = event.getGuild();
			Member member = guild == null || winner == null ? null : guild.getMember(winner);
			String username = member == null ? "Unknown User" : member.getEffectiveName();

			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle(String.format("%s has won the %s!", username, jam.getFullName()), winningSubmission.getSourceLink())
					.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
					.setDescription(String.format("> %s\nCheck out their project here:\n%s\nThey earned **%d** votes.", winningSubmission.getDescription(), winningSubmission.getSourceLink(), winningSubmissionVotes));
			votesMap.remove(winningSubmission);
			if (!votesMap.isEmpty()) {
				embedBuilder.addField("Also check out the other submissions:", null, false);
				for (Map.Entry<JamSubmission, Integer> entry : votesMap.entrySet()) {
					User user = event.getJDA().getUserById(entry.getKey().getUserId());
					Member runnerUpMember = guild == null || user == null ? null : guild.getMember(user);
					String runnerUpUsername = runnerUpMember == null ? "Unknown User" : runnerUpMember.getEffectiveName();
					embedBuilder.addField(
							runnerUpUsername + " with " + entry.getValue() + " votes",
							String.format("%s\n> %s", entry.getKey().getSourceLink(), entry.getKey().getDescription()),
							false
					);
				}
			}

			announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
			this.pingRole(announcementChannel, event);
		}
		this.removeAllMessages(votingChannel);
		new JamRepository(con).completeJam(jam);
	}

	private Map<JamTheme, Integer> recordThemeVotes(Jam jam, List<JamTheme> themes, Message themeVotingMessage) throws SQLException {
		Map<JamTheme, Integer> votes = new HashMap<>();
		themes.forEach(theme -> votes.put(theme, 0));
		PreparedStatement themeVoteStmt = con.prepareStatement("INSERT INTO jam_theme_vote (jam_id, user_id, theme_name) VALUES (?, ?, ?)");
		themeVoteStmt.setLong(1, jam.getId());
		for (int i = 0; i < themes.size(); i++) {
			JamTheme theme = themes.get(i);
			String reactionUnicode = REACTION_NUMBERS[i];
			List<User> users = themeVotingMessage.retrieveReactionUsers(reactionUnicode).complete();
			for (User user : users) {
				if (user.isBot()) continue;
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

	private void updateThemeAccepted(List<JamTheme> themes, JamTheme winner) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE jam_theme SET accepted = ? WHERE jam_id = ? AND name = ?");
		for (JamTheme theme : themes) {
			stmt.setBoolean(1, theme.equals(winner));
			stmt.setLong(2, theme.getJam().getId());
			stmt.setString(3, theme.getName());
			stmt.executeUpdate();
			theme.setAccepted(theme.equals(winner));
		}
		stmt.close();
	}

	private void sendChosenThemeMessage(TextChannel channel, Map<JamTheme, Integer> votes, JamTheme winner) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("The %s's Theme Has Been Chosen!", winner.getJam().getFullName()))
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription("The theme will be **" + winner.getName() + "**\n> " + winner.getDescription())
				.addField(
						"Submitting",
						"To submit your project for the Jam, simply use the `/jam submit` command." +
								" You'll need to provide a link to your project's source code, and a " +
								"short description. If you submit more than once, only your latest " +
								"submission is considered."
						,
						false
				)
				.addField("On behalf of Java-Discord's staff and helpers, we wish you the best of luck with your projects!", "*Also don't wait until the last minute to get started!*", false)
				.addBlankField(false)
				.addField("Here's how the votes played out:", "", false);
		for (Map.Entry<JamTheme, Integer> entry : votes.entrySet()) {
			embedBuilder.addField(entry.getKey().getName(), entry.getValue() + " votes", true);
		}
		channel.sendMessageEmbeds(embedBuilder.build()).complete();
	}

	private void removeAllMessages(TextChannel channel) {
		List<Message> messages;
		do {
			messages = channel.getHistory().retrievePast(50).complete();
			if (messages.isEmpty()) break;
			if (messages.size() == 1) {
				channel.deleteMessageById(messages.get(0).getIdLong()).complete();
			} else {
				channel.deleteMessages(messages).complete();
			}
		} while (!messages.isEmpty());
	}

	private void pingRole(TextChannel channel, SlashCommandEvent event) {
		Role jamPingRole = event.getJDA().getRoleById(Bot.getProperty("jamPingRoleId"));
		if (jamPingRole == null) {
			log.error("Could not find Jam ping role.");
			return;
		}
		channel.sendMessage(jamPingRole.getAsMention()).complete();
	}
}
