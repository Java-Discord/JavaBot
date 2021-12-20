package net.javadiscord.javabot.systems.jam;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamSubmission;
import net.javadiscord.javabot.systems.jam.model.JamTheme;
import net.javadiscord.javabot.util.Colors;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The channel manager is responsible for interacting with the various Discord
 * channels used by the Jam system, to send announcement, voting messages, and
 * to record votes.
 */
public class JamChannelManager {
	private final JamConfig config;

	/**
	 * Constructs the channel manager.
	 * @param jamConfig The config for the jam.
	 */
	public JamChannelManager(JamConfig jamConfig) {
		this.config = jamConfig;
	}

	/**
	 * Convenience method to send an error message as a response to a deferred
	 * slash command.
	 * @param event The slash command event.
	 * @param message The message to send.
	 */
	public void sendErrorMessageAsync(SlashCommandEvent event, String message) {
		event.getHook().sendMessage(message).queue();
	}

	/**
	 * Gets the number of votes each theme received.
	 * @param messageId The id of the message to which users have reacted to
	 *                  cast their votes.
	 * @param themes The list of themes that can be voted on.
	 * @return A map containing for each theme, a list of all user ids who have
	 * voted on that theme.
	 */
	public Map<JamTheme, List<Long>> getThemeVotes(long messageId, List<JamTheme> themes) {
		Message themeVotingMessage = this.config.getVotingChannel().retrieveMessageById(messageId).complete();
		Map<JamTheme, List<Long>> votes = new HashMap<>();
		for (int i = 0; i < themes.size(); i++) {
			List<User> users = themeVotingMessage.retrieveReactionUsers(JamPhaseManager.REACTION_NUMBERS[i]).complete();
			votes.put(
					themes.get(i),
					users.stream()
							.filter(user -> isUserVoteValid(themeVotingMessage.getGuild(), user, themeVotingMessage.getTimeCreated()))
							.map(ISnowflake::getIdLong)
							.toList()
			);
		}
		return votes;
	}

	/**
	 * Sends a message containing the themes to be voted on, in the voting channel.
	 * @param jam The jam for which the themes are prepared.
	 * @param themes The list of themes that will be put to a vote.
	 * @return The id of the message which was generated.
	 */
	public long sendThemeVotingMessages(Jam jam, List<JamTheme> themes) {
		this.removeAllMessages(this.config.getVotingChannel());
		EmbedBuilder voteEmbedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s Theme Voting", jam.getFullName()))
				.setColor(this.config.getJamEmbedColor())
				.setDescription("Vote for your preferred themes for the upcoming Jam.");
		for (int i = 0; i < themes.size(); i++) {
			JamTheme theme = themes.get(i);
			voteEmbedBuilder.addField(JamPhaseManager.REACTION_NUMBERS[i] + " " + theme.getName(), theme.getDescription(), false);
		}
		Message themeVoteMessage = this.config.getVotingChannel().sendMessageEmbeds(voteEmbedBuilder.build()).complete();
		for (int i = 0; i < themes.size(); i++) {
			themeVoteMessage.addReaction(JamPhaseManager.REACTION_NUMBERS[i]).complete();
		}
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s Theme Voting Has Started!", jam.getFullName()))
				.setColor(this.config.getJamEmbedColor())
				.setDescription("Go to " + this.config.getVotingChannel().getAsMention() + " to cast your votes, and decide what theme will be chosen for the Jam!");
		this.config.getAnnouncementChannel().sendMessageEmbeds(embedBuilder.build()).complete();
		this.pingRole();
		return themeVoteMessage.getIdLong();
	}

	/**
	 * Sends a message in the announcement channel which shows the theme which
	 * was chosen.
	 * @param votes A map containing for each theme, the number of votes it got.
	 * @param winner The theme which was most voted-for.
	 */
	public void sendChosenThemeMessage(Map<JamTheme, Integer> votes, JamTheme winner) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("The %s's Theme Has Been Chosen!", winner.getJam().getFullName()))
				.setColor(this.config.getJamEmbedColor())
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
		this.config.getAnnouncementChannel().sendMessageEmbeds(embedBuilder.build()).queue();
		this.removeAllMessages(this.config.getVotingChannel());
		this.pingRole();
	}

	/**
	 * Sends a message in the Jam voting channel for each submission, so that
	 * users can begin voting on submissions.
	 * @param jam The jam that the submissions are for.
	 * @param submissions The submissions to make a message for.
	 * @param jda The JDA instance.
	 * @return A map containing for every submission, the id of the message that
	 * users will react with votes on.
	 */
	public Map<JamSubmission, Long> sendSubmissionVotingMessage(Jam jam, List<JamSubmission> submissions, JDA jda) {
		this.removeAllMessages(this.config.getVotingChannel());
		Map<JamSubmission, Long> messageIds = new HashMap<>();
		for (JamSubmission submission : submissions) {
			User user = jda.getUserById(submission.getUserId());
			String userName = user == null ? "Unknown user" : user.getAsTag();
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Submission by " + userName, submission.getSourceLink())
					.setColor(Colors.randomPastel())
					.setTimestamp(submission.getCreatedAt())
					.addField("Description", submission.getDescription(), false);
			Message message = this.config.getVotingChannel().sendMessageEmbeds(embedBuilder.build()).complete();
			message.addReaction(JamPhaseManager.SUBMISSION_VOTE_UNICODE).complete();
			messageIds.put(submission, message.getIdLong());
		}

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Voting Has Begun!")
				.setColor(this.config.getJamEmbedColor())
				.setDescription(String.format("Go to %s to vote for who you think should win the %s.", this.config.getVotingChannel().getAsMention(), jam.getFullName()));
		this.config.getAnnouncementChannel().sendMessageEmbeds(embedBuilder.build()).queue();
		this.pingRole();
		return messageIds;
	}

	/**
	 * Gets a list of user ids for each Jam submission, indicating the list of
	 * users that voted for the submission.
	 * @param submissionMessageMap A map containing for each submission, the id
	 *                             of the message on which users will react with
	 *                             the vote emoji.
	 * @return A map containing a list of user ids for each Jam submission.
	 */
	public Map<JamSubmission, List<Long>> getSubmissionVotes(Map<JamSubmission, Long> submissionMessageMap) {
		Map<JamSubmission, List<Long>> votesMap = new HashMap<>();
		OffsetDateTime cutoff = OffsetDateTime.now();
		for (var entry : submissionMessageMap.entrySet()) {
			Message message = this.config.getVotingChannel().retrieveMessageById(entry.getValue()).complete();
			Guild guild = message.getGuild();
			List<User> users = message.retrieveReactionUsers(JamPhaseManager.SUBMISSION_VOTE_UNICODE).complete();
			votesMap.put(
					entry.getKey(),
					users.stream()
							.filter(user -> isUserVoteValid(guild, user, cutoff))
							.map(ISnowflake::getIdLong)
							.toList()
			);
		}
		return votesMap;
	}

	/**
	 * Determines if a user's vote should be counted, based on how long the user
	 * has been a member of the guild, and how active they are.
	 * @param guild The guild in which to check.
	 * @param user The user to check.
	 * @param cutoff The date at which the vote is being performed.
	 * @return True if the user's vote should count, or false if not.
	 */
	public boolean isUserVoteValid(Guild guild, User user, OffsetDateTime cutoff) {
		if (user.isBot() || user.isSystem()) return false;
		Member member = guild.getMember(user);
		if (member == null || member.isPending()) return false;
		boolean memberForSufficientTime = (!member.hasTimeJoined() || member.getTimeJoined().plusHours(1).isBefore(cutoff));
		boolean sentMessage = false;
		for (var channel : guild.getTextChannels()) {
			sentMessage = sentMessage || hasMemberSentMessage(member, channel, cutoff.minusMonths(1));
		}
		return memberForSufficientTime && sentMessage;
	}

	/**
	 * Determines if the given member has sent at least a single message in the
	 * given channel.
	 * @param member The member to check.
	 * @param channel The channel to check in.
	 * @param cutoff The time at which messages are considered. Any message
	 *               before this time is ignored.
	 * @return True if the user has sent a message, or false otherwise.
	 */
	public boolean hasMemberSentMessage(Member member, TextChannel channel, OffsetDateTime cutoff) {
		MessageHistory history = channel.getHistory();
		List<Message> messages = history.retrievePast(100).complete();
		while (!messages.isEmpty()) {
			for (var message : messages) {
				if (message.getTimeCreated().isBefore(cutoff)) {
					break;
				} else if (member.equals(message.getMember())) {
					return true;
				}
			}
			// No message was found, and we haven't reached the cutoff, so fetch more.
			messages = history.retrievePast(100).complete();
		}
		return false;
	}

	/**
	 * Sends a message in the announcement channel in the event that no winners
	 * could be determined for a jam.
	 */
	public void sendNoWinnersMessage() {
		this.config.getAnnouncementChannel().sendMessage("No winning submission could be determined.").queue();
	}

	/**
	 * Sends a message in the announcement channel when a single winner is
	 * chosen for a jam.
	 * @param submission The winning submission.
	 * @param voteCounts A map containing for each submission, the number of
	 *                   votes it received.
	 * @param event The event which triggered this method.
	 */
	public void sendSingleWinnerMessage(JamSubmission submission, Map<JamSubmission, Integer> voteCounts, SlashCommandEvent event) {
		String username = this.getSubmissionUserName(submission, event);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("%s has won the %s!", username, submission.getJam().getFullName()), submission.getSourceLink())
				.setColor(this.config.getJamEmbedColor())
				.setDescription(String.format("> %s\nCheck out their project here:\n%s\nThey earned **%d** votes.", submission.getDescription(), submission.getSourceLink(), voteCounts.get(submission)));
		this.addRunnerUpSubmissionFields(embedBuilder, voteCounts, List.of(submission), event);

		this.config.getAnnouncementChannel().sendMessageEmbeds(embedBuilder.build()).queue();
		this.pingRole();
		this.removeAllMessages(this.config.getVotingChannel());
	}

	/**
	 * Sends a message in the announcement channel when multiple winners are
	 * chosen for a jam.
	 * @param submissions The list of winning submissions.
	 * @param voteCounts A map containing for each submission, the number of
	 *                   votes it received.
	 * @param event The event which triggered this method.
	 */
	public void sendMultipleWinnersMessage(List<JamSubmission> submissions, Map<JamSubmission, Integer> voteCounts, SlashCommandEvent event) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle(String.format("There Are Multiple Winners of the %s!", submissions.get(0).getJam()))
				.setColor(this.config.getJamEmbedColor());
		for (var submission : submissions) {
			String username = this.getSubmissionUserName(submission, event);
			embedBuilder.addField(
					username + " with " + voteCounts.get(submission) + " votes",
					String.format("%s\n> %s", submission.getSourceLink(), submission.getDescription()),
					false
			);
		}
		this.addRunnerUpSubmissionFields(embedBuilder, voteCounts, submissions, event);

		this.config.getAnnouncementChannel().sendMessageEmbeds(embedBuilder.build()).queue();
		this.pingRole();
		this.removeAllMessages(this.config.getVotingChannel());
	}

	/**
	 * Adds fields to an embed for displaying information about the runners-up
	 * for a jam.
	 * @param embedBuilder The embed builder.
	 * @param voteCounts A map containing each submission and the number of
	 *                   votes it received.
	 * @param winners The list of winning submissions.
	 * @param event The event which triggered this method.
	 */
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

	/**
	 * Gets the name which should be displayed for a user's submission. This
	 * defaults to the user's guild-specific nickname, or "Unknown User" if no
	 * name could be resolved.
	 * @param submission The submission to get a username for.
	 * @param event The event which triggered this method.
	 * @return The name to display alongside the submission.
	 */
	private String getSubmissionUserName(JamSubmission submission, SlashCommandEvent event) {
		User winner = event.getJDA().getUserById(submission.getUserId());
		Guild guild = event.getGuild();
		Member member = guild == null || winner == null ? null : guild.getMember(winner);
		return member == null ? "Unknown User" : member.getEffectiveName();
	}

	/**
	 * Utility method to remove all messages from a channel.
	 * @param channel The channel to remove messages from.
	 */
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

	/**
	 * Sends a single message in the announcement channel that contains a "ping"
	 * for the jam-ping role, to alert all members of that role.
	 */
	private void pingRole() {
		this.config.getAnnouncementChannel().sendMessage(this.config.getPingRole().getAsMention()).queue();
	}
}
