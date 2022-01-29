package net.javadiscord.javabot.systems.qotw.submissions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubcommand;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.GuildUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles and manages Submission controls.
 */
@Slf4j
@RequiredArgsConstructor
public class SubmissionControlsManager {
	private final String SUBMISSION_ACCEPTED = "✅";
	private final String SUBMISSION_DECLINED = "❌";
	private final String SUBMISSION_PENDING = "\uD83D\uDD52";

	private final Guild guild;
	private final QOTWConfig config;
	private final QOTWSubmission submission;

	/**
	 * Sends an embed in the submission's guild channel that allows QOTW-Review Team members to accept, decline or delete submissions.
	 *
	 * @return Whether the message was successfully delivered.
	 */
	public boolean sendSubmissionControls() {
		var thread = this.guild.getThreadChannelById(this.submission.getThreadId());
		if (thread == null) return false;
		removeThreadMembers(thread, this.config);
		thread.getManager().setName(String.format("%s %s", SUBMISSION_PENDING, thread.getName())).queue();
		thread.sendMessage(config.getQOTWReviewRole().getAsMention())
				.setEmbeds(buildSubmissionControlEmbed())
				.setActionRows(buildInteractionControls()).queue();
		log.info("Sent Submission Controls to thread {}", thread.getName());
		return true;
	}

	private void removeThreadMembers(ThreadChannel thread, QOTWConfig config) {
		thread.getThreadMembers()
				.stream()
				.filter(m -> !m.getMember().getRoles().contains(config.getQOTWReviewRole()) && !m.getMember().getUser().isBot())
				.forEach(m -> thread.removeThreadMember(m.getUser()).queue());
	}

	private boolean hasPermissions(Member member) {
		return !member.getRoles().isEmpty() && member.getRoles().contains(config.getQOTWReviewRole());
	}

	/**
	 * Handles Button interactions regarding the Submission Controls System.
	 *
	 * @param id    The button's id, split by ":".
	 * @param event The {@link ButtonClickEvent} that is fired upon use.
	 */
	public void handleButtons(String[] id, ButtonClickEvent event) {
		if (!hasPermissions(event.getMember())) {
			event.getHook().sendMessage("Insufficient Permissions.").setEphemeral(true).queue();
			return;
		}
		if (!event.getChannelType().isThread()) {
			event.getHook().sendMessage("This interaction may only be used in thread channels.").setEphemeral(true).queue();
			return;
		}
		var thread = (ThreadChannel) event.getGuildChannel();
		switch (id[1]) {
			case "accept" -> acceptSubmission(event, thread);
			case "decline" -> declineButtonSubmission(event);
			case "delete" -> deleteSubmission(event, thread);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	/**
	 * Handles Select Menu interactions regarding the Submission Controls System.
	 *
	 * @param id    The SelectionMenu's id.
	 * @param event The {@link SelectionMenuEvent} that is fired upon use.
	 */
	public void handleSelectMenus(String[] id, SelectionMenuEvent event) {
		if (!hasPermissions(event.getMember())) {
			event.getHook().sendMessage("Insufficient Permissions.").setEphemeral(true).queue();
			return;
		}
		if (!event.getChannelType().isThread()) {
			event.getHook().sendMessage("This interaction may only be used in thread channels.").setEphemeral(true).queue();
			return;
		}
		var thread = (ThreadChannel) event.getGuildChannel();
		switch (id[1]) {
			case "decline" -> declineSelectSubmission(event, thread);
			default -> Responses.error(event.getHook(), "Unknown Interaction").queue();
		}
	}

	private void acceptSubmission(ButtonClickEvent event, ThreadChannel thread) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QOTWSubmissionRepository(con);
			var submissionOptional = repo.getSubmissionByThreadId(thread.getIdLong());
			if (submissionOptional.isEmpty()) return;
			var submission = submissionOptional.get();
			repo.markReviewed(submission);
			repo.markAccepted(submission);
			event.getGuild().retrieveMemberById(submission.getAuthorId()).queue(
					member -> {
						if (member == null) {
							Responses.error(event.getHook(), "Cannot accept a submission of a user who is not a member of this server");
							return;
						}
						new IncrementSubcommand().correct(member, true);
						thread.getManager().setName(SUBMISSION_ACCEPTED + thread.getName().substring(1)).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
						log.info("{} accepted {}'s submission", event.getUser().getAsTag(), member.getUser().getAsTag());
						GuildUtils.getLogChannel(event.getGuild()).sendMessageFormat("%s accepted %s's submission", event.getUser().getAsTag(), member.getUser().getAsTag()).queue();
						this.disableControls(String.format("Accepted by %s", event.getUser().getAsTag()), event.getMessage());
						Responses.success(event.getHook(), "Submission Accepted", "Successfully accepted submission by " + member.getAsMention()).queue();
					}
			);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void declineButtonSubmission(ButtonClickEvent event) {
		event.getMessage().editMessageComponents(ActionRow.of(this.buildDeclineMenu())).queue();
	}

	private void declineSelectSubmission(SelectionMenuEvent event, ThreadChannel thread) {
		var reasons = String.join(", ", event.getValues());
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QOTWSubmissionRepository(con);
			var submissionOptional = repo.getSubmissionByThreadId(thread.getIdLong());
			if (submissionOptional.isEmpty()) return;
			var submission = submissionOptional.get();
			repo.markReviewed(submission);
			event.getGuild().retrieveMemberById(submission.getAuthorId()).queue(
					member -> {
						if (member == null) {
							Responses.error(event.getHook(), "Cannot accept a submission of a user who is not a member of this server");
							return;
						}
						member.getUser().openPrivateChannel().queue(
								c -> c.sendMessageEmbeds(buildSubmissionDeclinedEmbed(member.getUser(), reasons)).queue(),
								e -> {
								});
						thread.getManager().setName(SUBMISSION_DECLINED + thread.getName().substring(1)).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
						log.info("{} declined {}'s submission for: {}", event.getUser().getAsTag(), member.getUser().getAsTag(), reasons);
						GuildUtils.getLogChannel(event.getGuild()).sendMessageFormat("%s declined %s's submission for: `%s`", event.getUser().getAsTag(), member.getUser().getAsTag(), reasons).queue();
						this.disableControls(String.format("Declined by %s", event.getUser().getAsTag()), event.getMessage());
						Responses.success(event.getHook(), "Submission Declined",
								String.format("Successfully declined submission by %s for the following reasons:\n`%s`" + member.getAsMention(), reasons)).queue();
					}
			);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void deleteSubmission(ButtonClickEvent event, ThreadChannel thread) {
		thread.delete().queueAfter(10, TimeUnit.SECONDS);
		log.info("{} deleted submission thread {}", event.getUser().getAsTag(), thread.getName());
		GuildUtils.getLogChannel(event.getGuild()).sendMessageFormat("%s deleted submission thread `%s`", event.getUser().getAsTag(), thread.getName()).queue();
		this.disableControls(String.format("Deleted by %s", event.getUser().getAsTag()), event.getMessage());
		event.getHook().sendMessage("Submission will be deleted in 10 seconds...").setEphemeral(true);
	}

	private void disableControls(String buttonLabel, Message message) {
		message.editMessageComponents(ActionRow.of(Button.secondary("submission-controls:dummy", buttonLabel).asDisabled())).queue();
	}

	private MessageEmbed buildSubmissionDeclinedEmbed(User createdBy, String reasons) {
		return new EmbedBuilder()
				.setTitle("QOTW Notification")
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setColor(Bot.config.get(config.getGuild()).getSlashCommand().getErrorColor())
				.setDescription(String.format("""
								Hey %s,
								Your QOTW-Submission was **declined** for the following reasons:
								**`%s`**

								However, you can try your luck again next week!""",
						createdBy.getAsMention(), reasons))
				.setTimestamp(Instant.now())
				.build();
	}

	private List<ActionRow> buildInteractionControls() {
		return List.of(ActionRow.of(
				Button.success("submission-controls:accept", "Accept"),
				Button.danger("submission-controls:decline", "Decline"),
				Button.secondary("submission-controls:delete", "🗑️")));
	}

	private SelectionMenu buildDeclineMenu() {
		return SelectionMenu.create("submission-controls:decline")
				.setPlaceholder("Select a reason for declining this submission.")
				.setRequiredRange(1, 3)
				.addOption("Wrong Answer", "Wrong Answer", "The content of the submission was not correct.")
				.addOption("Incomplete Answer", "Incomplete Answer", "The submission was missing some important things and was overall incomplete.")
				.addOption("Too short", "Too short", "The submission was way too short in comparison to other submissions.")
				.build();
	}

	private MessageEmbed buildSubmissionControlEmbed() {
		return new EmbedBuilder()
				.setTitle("Submission Controls")
				.setDescription("Please choose an action for this Submission.")
				.setTimestamp(Instant.now())
				.build();
	}
}
