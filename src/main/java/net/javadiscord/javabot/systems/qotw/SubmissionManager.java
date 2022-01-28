package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubcommand;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Handles & manages QOTW Submissions by using Discords {@link ThreadChannel}s.
 */
@Slf4j
@RequiredArgsConstructor
public class SubmissionManager {
	/**
	 * The submission thread's name.
	 */
	public static final String THREAD_NAME = "[#%s] %s | %s";
	private final String SUBMISSION_ACCEPTED = "✅";
	private final String SUBMISSION_DECLINED = "❌";

	private final QOTWConfig config;

	/**
	 * Handles the "Submit your Answer" Button interaction.
	 *
	 * @param event          The {@link ButtonClickEvent} that is fired upon use.
	 * @param questionNumber The current qotw-week number.
	 * @return A {@link WebhookMessageAction}.
	 */
	public WebhookMessageAction<?> handleSubmission(ButtonClickEvent event, long questionNumber) {
		if (!isLatestQOTWMessage(event.getMessage())) {
			return Responses.error(event.getHook(), "You may only answer the newest QOTW.");
		}
		var member = event.getMember();
		if (hasActiveSubmissionThread(member, questionNumber)) {
			var thread = getSubmissionThread(member, questionNumber);
			return Responses.error(event.getHook(), "You already have a submission thread: " + thread.get().getAsMention());
		}
		if (!canCreateSubmissions(event.getMember(), questionNumber)) {
			return Responses.warning(event.getHook(), "You're not eligible to create a new submission thread.");
		}
		config.getSubmissionChannel().createThreadChannel(
				String.format(THREAD_NAME, questionNumber, member.getEffectiveName(), member.getId()), true).queue(
				thread -> {
					thread.getManager().setInvitable(false).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue();
					thread.sendMessage(String.format("%s, %s", event.getUser().getAsMention(), config.getQOTWReviewRole().getAsMention()))
							.setEmbeds(buildSubmissionThreadEmbed(event.getUser(), questionNumber, config))
							.setActionRows(ActionRow.of(Button.danger("qotw-submission:delete", "Delete Submission")))
							.queue();
				}, e -> log.error("Could not create submission thread for member {}. ", member.getUser().getAsTag(), e)
		);
		log.info("Opened new Submission Thread for User {}", member.getUser().getAsTag());
		return Responses.success(event.getHook(), "Submission Thread created",
				"Successfully created a new private Thread for your submission.");
	}

	/**
	 * Handles the "Delete Submission" Button.
	 *
	 * @param event The {@link ButtonClickEvent} that is fired upon use.
	 */
	public void handleThreadDeletion(ButtonClickEvent event) {
		var thread = (ThreadChannel) event.getGuildChannel();
		if (thread.getName().contains(event.getUser().getId())) {
			thread.delete().queue();
			log.info("Deleted {}'s Submission Thread", event.getUser().getAsTag());
		}
	}

	private boolean isLatestQOTWMessage(Message message) {
		Message latestMessage = null;
		var history = message.getChannel().getHistory();
		while (latestMessage == null) {
			var messages = history.retrievePast(100).complete();
			for (var m : messages) {
				if (m.getAuthor().getIdLong() == m.getJDA().getSelfUser().getIdLong()) {
					latestMessage = m;
					break;
				}
			}
		}
		return message.equals(latestMessage);
	}

	private boolean canCreateSubmissions(Member member, long questionNumber) {
		if (member == null) return false;
		if (member.getUser().isBot() || member.getUser().isSystem()) return false;
		if (member.isTimedOut() || member.isPending()) return false;
		return !hasActiveSubmissionThread(member, questionNumber);
	}

	public boolean hasActiveSubmissionThread(Member member, long questionNumber) {
		var optional = getSubmissionThread(member, questionNumber);
		return optional.isPresent() && !optional.get().isArchived();
	}

	/**
	 * Tries to retrieve the owner of this submission by using the id that is embedded into the channel name.
	 *
	 * @param channel The {@link ThreadChannel}.
	 * @return The submission's owner.
	 */
	public Member getSubmissionThreadOwner(ThreadChannel channel) {
		var split = channel.getName().split("\\s+");
		var userId = split[split.length - 1];
		return channel.getGuild().getMemberById(userId);
	}

	/**
	 * Gets the given member's submission thread.
	 *
	 * @param member         The member whose thread should be retrieved.
	 * @param questionNumber The current qotw-week number.
	 * @return The {@link ThreadChannel} as an {@link Optional}.
	 */
	public Optional<ThreadChannel> getSubmissionThread(Member member, long questionNumber) {
		return config.getSubmissionChannel().getThreadChannels()
				.stream()
				.filter(s -> s.getName().contains(String.format(THREAD_NAME, questionNumber, member.getEffectiveName(), member.getId())) && !s.isArchived())
				.findFirst();
	}

	/**
	 * Handles Button interactions regarding the Submission Controls System.
	 *
	 * @param id    The button's id, split by ":".
	 * @param event The {@link ButtonClickEvent} that is fired upon use.
	 * @return The {@link WebhookMessageAction}.
	 */
	public WebhookMessageAction<?> handleSubmissionControlButtonInteractions(String[] id, ButtonClickEvent event) {
		if (!event.getMember().getRoles().isEmpty() && !event.getMember().getRoles().contains(config.getQOTWReviewRole())) {
			return event.getHook().sendMessage("Insufficient Permissions.").setEphemeral(true);
		}
		if (!event.getChannelType().isThread()) {
			return event.getHook().sendMessage("This interaction may only be used in thread channels.").setEphemeral(true);
		}
		var thread = (ThreadChannel) event.getGuildChannel();
		return switch (id[1]) {
			case "accept" -> acceptSubmission(event, thread);
			case "delete" -> deleteSubmission(event, thread);
			default -> event.getHook().sendMessage("Invalid Interaction").setEphemeral(true);
		};
	}

	/**
	 * Handles Select Menu interactions regarding the Submission Controls System.
	 *
	 * @param id    The SelectionMenu's id.
	 * @param event The {@link SelectionMenuEvent} that is fired upon use.
	 * @return The {@link WebhookMessageAction}.
	 */
	public WebhookMessageAction<?> handleSubmissionControlSelectInteraction(String id, SelectionMenuEvent event) {
		if (!event.getMember().getRoles().isEmpty() && !event.getMember().getRoles().contains(config.getQOTWReviewRole())) {
			return event.getHook().sendMessage("Insufficient Permissions.").setEphemeral(true);
		}
		if (!event.getChannelType().isThread()) {
			return event.getHook().sendMessage("This interaction may only be used in thread channels.").setEphemeral(true);
		}
		var thread = (ThreadChannel) event.getGuildChannel();
		if (id.equals("submission-controls:decline")) {
			return declineSubmission(event, thread);
		} else {
			return event.getHook().sendMessage("Invalid Interaction").setEphemeral(true);
		}
	}

	private WebhookMessageAction<?> acceptSubmission(ButtonClickEvent event, ThreadChannel thread) {
		var member = getSubmissionThreadOwner(thread);
		if (member == null) {
			return event.getHook().sendMessage("Cannot accept a submission of a user who is not a member of this server");
		}
		new IncrementSubcommand().correct(member, true);
		thread.getManager().setName(SUBMISSION_ACCEPTED + thread.getName().substring(1)).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
		log.info("{} accepted {}'s submission", event.getUser().getAsTag(), member.getUser().getAsTag());
		this.disableControls(String.format("Accepted by %s", event.getUser().getAsTag()), event.getMessage());
		return event.getHook().sendMessage("Successfully accepted submission by " + member.getAsMention()).setEphemeral(true);
	}

	private WebhookMessageAction<?> declineSubmission(SelectionMenuEvent event, ThreadChannel thread) {
		var member = getSubmissionThreadOwner(thread);
		var reasons = String.join(", ", event.getValues());
		if (member == null) {
			return event.getHook().sendMessage("Cannot decline a submission of a user who is not a member of this server");
		}
		thread.getManager().setName(SUBMISSION_DECLINED + thread.getName().substring(1)).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
		member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(buildSubmissionDeclinedEmbed(member.getUser(), reasons)).queue(), e -> {
		});
		log.info("{} declined {}'s submission for: {}", event.getUser().getAsTag(), member.getUser().getAsTag(), reasons);
		this.disableControls(String.format("Declined by %s for: %s", event.getUser().getAsTag(), reasons), event.getMessage());
		return event.getHook().sendMessage("Successfully declined submission by " + member.getAsMention()).setEphemeral(true);
	}

	private WebhookMessageAction<?> deleteSubmission(ButtonClickEvent event, ThreadChannel thread) {
		thread.delete().queueAfter(10, TimeUnit.SECONDS);
		log.info("{} deleted submission {}", event.getUser().getAsTag(), thread.getName());
		this.disableControls(String.format("Deleted by %s", event.getUser().getAsTag()), event.getMessage());
		return event.getHook().sendMessage("Submission will be deleted in 10 seconds...").setEphemeral(true);
	}

	private void disableControls(String buttonLabel, Message message) {
		message.editMessageComponents(ActionRow.of(Button.secondary("submission-controls:dummy", buttonLabel).asDisabled())).queue();
	}

	private MessageEmbed buildSubmissionThreadEmbed(User createdBy, long questionNumber, QOTWConfig config) {
		return new EmbedBuilder()
				.setColor(Bot.config.get(config.getGuild()).getSlashCommand().getDefaultColor())
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle(String.format("Question of the Week #%s", questionNumber))
				.setDescription(String.format("Hey, %s! Please submit your answer into this thread." +
								"\nYou can even send multiple messages, if you want to. This whole thread counts as your submission." +
								"\nThe %s will review your submission once a new question appears.",
						createdBy.getAsMention(), config.getQOTWReviewRole().getAsMention()))
				.setTimestamp(Instant.now())
				.build();
	}

	private MessageEmbed buildSubmissionDeclinedEmbed(User createdBy, String reasons) {
		return new EmbedBuilder()
				.setTitle("QOTW Notification")
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setColor(Bot.config.get(config.getGuild()).getSlashCommand().getErrorColor())
				.setDescription(String.format(
						"Hey %s," +
								"\nYour QOTW-Submission was **declined** for the following reasons:" +
								"\n**`%s`**" +
								"\n\nHowever, you can try your luck again next week!", createdBy.getAsMention(), reasons))
				.setTimestamp(Instant.now())
				.build();
	}
}
