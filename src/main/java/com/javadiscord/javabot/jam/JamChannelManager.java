package com.javadiscord.javabot.jam;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamSubmission;
import com.javadiscord.javabot.jam.model.JamTheme;
import com.javadiscord.javabot.other.Colors;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JamChannelManager {
	private static final Logger log = LoggerFactory.getLogger(JamChannelManager.class);

	private final TextChannel votingChannel;
	private final TextChannel announcementChannel;

	public JamChannelManager(Guild guild) {
		this.votingChannel = Database.getConfigChannel(guild, "channels.jam_vote_cid");
		this.announcementChannel = Database.getConfigChannel(guild, "channels.jam_announcement_cid");
	}

	public void sendErrorMessageAsync(SlashCommandEvent event, String message) {
		event.getHook().sendMessage(message).queue();
	}

	public Map<JamTheme, List<Long>> getThemeVotes(long messageId, List<JamTheme> themes) {
		Message themeVotingMessage = votingChannel.retrieveMessageById(messageId).complete();
		Map<JamTheme, List<Long>> votes = new HashMap<>();
		for (int i = 0; i < themes.size(); i++) {
			List<User> users = themeVotingMessage.retrieveReactionUsers(JamPhaseManager.REACTION_NUMBERS[i]).complete();
			votes.put(themes.get(i), users.stream().filter(user -> !user.isBot()).map(ISnowflake::getIdLong).collect(Collectors.toList()));
		}
		return votes;
	}

	public long sendThemeVotingMessages(Jam jam, List<JamTheme> themes) {
		this.removeAllMessages(this.votingChannel);
		EmbedBuilder voteEmbedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s Theme Voting", jam.getFullName()))
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription("Vote for your preferred themes for the upcoming Jam.");
		for (int i = 0; i < themes.size(); i++) {
			JamTheme theme = themes.get(i);
			voteEmbedBuilder.addField(JamPhaseManager.REACTION_NUMBERS[i] + " " + theme.getName(), theme.getDescription(), false);
		}
		Message themeVoteMessage = votingChannel.sendMessageEmbeds(voteEmbedBuilder.build()).complete();
		for (int i = 0; i < themes.size(); i++) {
			themeVoteMessage.addReaction(JamPhaseManager.REACTION_NUMBERS[i]).complete();
		}
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s Theme Voting Has Started!", jam.getFullName()))
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription("Go to " + votingChannel.getAsMention() + " to cast your votes, and decide what theme will be chosen for the Jam!");
		announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole();
		return themeVoteMessage.getIdLong();
	}

	public void sendChosenThemeMessage(Map<JamTheme, Integer> votes, JamTheme winner) {
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
		this.announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.removeAllMessages(votingChannel);
		this.pingRole();
	}

	public Map<JamSubmission, Long> sendSubmissionVotingMessage(Jam jam, List<JamSubmission> submissions, JDA jda) {
		this.removeAllMessages(this.votingChannel);
		Map<JamSubmission, Long> messageIds = new HashMap<>();
		for (JamSubmission submission : submissions) {
			User user = jda.getUserById(submission.getUserId());
			String userName = user == null ? "Unknown user" : user.getAsTag();
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Submission by " + userName, submission.getSourceLink())
					.setColor(Colors.randomPastel())
					.setTimestamp(submission.getCreatedAt())
					.addField("Description", submission.getDescription(), false);
			Message message = votingChannel.sendMessageEmbeds(embedBuilder.build()).complete();
			message.addReaction(JamPhaseManager.SUBMISSION_VOTE_UNICODE).complete();
			messageIds.put(submission, message.getIdLong());
		}

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Voting Has Begun!")
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription(String.format("Go to %s to vote for who you think should win the %s.", votingChannel.getAsMention(), jam.getFullName()));
		announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole();
		return messageIds;
	}

	public Map<JamSubmission, List<Long>> getSubmissionVotes(Map<JamSubmission, Long> submissionMessageMap) {
		Map<JamSubmission, List<Long>> votesMap = new HashMap<>();
		for (var entry : submissionMessageMap.entrySet()) {
			Message message = votingChannel.retrieveMessageById(entry.getValue()).complete();
			List<User> users = message.retrieveReactionUsers(JamPhaseManager.SUBMISSION_VOTE_UNICODE).complete();
			votesMap.put(entry.getKey(), users.stream().filter(user -> !user.isBot()).map(ISnowflake::getIdLong).collect(Collectors.toList()));
		}
		return votesMap;
	}

	public void sendNoWinnersMessage() {
		announcementChannel.sendMessage("No winning submission could be determined.").complete();
	}

	public void sendSingleWinnerMessage(JamSubmission submission, Map<JamSubmission, Integer> voteCounts, SlashCommandEvent event) {
		String username = this.getSubmissionUserName(submission, event);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s has won the %s!", username, submission.getJam().getFullName()), submission.getSourceLink())
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.setDescription(String.format("> %s\nCheck out their project here:\n%s\nThey earned **%d** votes.", submission.getDescription(), submission.getSourceLink(), voteCounts.get(submission)));
		this.addRunnerUpSubmissionFields(embedBuilder, voteCounts, List.of(submission), event);

		this.announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole();
		this.removeAllMessages(this.votingChannel);
	}

	public void sendMultipleWinnersMessage(List<JamSubmission> submissions, Map<JamSubmission, Integer> voteCounts, SlashCommandEvent event) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("There Are Multiple Winners of the %s!", submissions.get(0).getJam()))
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")));
		for (var submission : submissions) {
			String username = this.getSubmissionUserName(submission, event);
			embedBuilder.addField(
					username + " with " + voteCounts.get(submission) + " votes",
					String.format("%s\n> %s", submission.getSourceLink(), submission.getDescription()),
					false
			);
		}
		this.addRunnerUpSubmissionFields(embedBuilder, voteCounts, submissions, event);

		this.announcementChannel.sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole();
		this.removeAllMessages(this.votingChannel);
	}

	private void addRunnerUpSubmissionFields(EmbedBuilder embedBuilder, Map<JamSubmission, Integer> voteCounts, List<JamSubmission> winners, SlashCommandEvent event) {
		var otherSubmissions = new HashMap<>(voteCounts);
		winners.forEach(otherSubmissions::remove);
		if (!otherSubmissions.isEmpty()) {
			embedBuilder.addField("Also check out the other submissions:", "", false);
			for (var entry : otherSubmissions.entrySet()) {
				String runnerUpUsername = this.getSubmissionUserName(entry.getKey(), event);
				embedBuilder.addField(
						runnerUpUsername + " with " + entry.getValue() + " votes",
						String.format("%s\n> %s", entry.getKey().getSourceLink(), entry.getKey().getDescription()),
						false
				);
			}
		}
	}

	private String getSubmissionUserName(JamSubmission submission, SlashCommandEvent event) {
		User winner = event.getJDA().getUserById(submission.getUserId());
		Guild guild = event.getGuild();
		Member member = guild == null || winner == null ? null : guild.getMember(winner);
		return member == null ? "Unknown User" : member.getEffectiveName();
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

	private void pingRole() {
		Role jamPingRole = Database.getConfigRole(this.announcementChannel.getGuild(), "roles.jam_ping_rid");
		if (jamPingRole == null) {
			log.error("Could not find Jam ping role.");
			return;
		}
		this.announcementChannel.sendMessage(jamPingRole.getAsMention()).queue();
	}
}
