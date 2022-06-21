package net.javadiscord.javabot.systems.qotw.submissions;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.notification.QOTWNotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Handles and manages Submission controls.
 */
@Slf4j
public class SubmissionControlsManager {
	private static final String SUBMISSION_ACCEPTED = "\u2705";
	private static final String SUBMISSION_DECLINED = "\u274C";
	private static final String SUBMISSION_PENDING = "\uD83D\uDD52";

	private final Guild guild;
	private final QOTWConfig config;
	private final QOTWSubmission submission;

	/**
	 * The constructor of this class.
	 *
	 * @param guild      The current {@link Guild}.
	 * @param submission The {@link QOTWSubmission}.
	 */
	public SubmissionControlsManager(Guild guild, QOTWSubmission submission) {
		this.guild = guild;
		this.submission = submission;
		this.config = Bot.config.get(guild).getQotw();
	}

	/**
	 * The constructor of this class.
	 *
	 * @param guild   The current {@link Guild}.
	 * @param channel The {@link ThreadChannel}, which is used to retrieve the corresponding {@link QOTWSubmission}.
	 */
	public SubmissionControlsManager(Guild guild, ThreadChannel channel) {
		QOTWSubmission submission = null;
		try (Connection con = Bot.dataSource.getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			Optional<QOTWSubmission> submissionOptional = repo.getSubmissionByThreadId(channel.getIdLong());
			if (submissionOptional.isEmpty()) {
				log.error("Could not retrieve Submission from Thread: " + channel.getId());
			} else {
				submission = submissionOptional.get();
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
		this.guild = guild;
		this.submission = submission;
		this.config = Bot.config.get(guild).getQotw();
	}

	/**
	 * Sends an embed in the submission's thread channel that allows QOTW-Reviewers to perform various actions.
	 */
	public void sendControls() {
		ThreadChannel thread = this.guild.getThreadChannelById(this.submission.getThreadId());
		if (thread == null) return;
		// The Thread's starting message
		if (thread.getMessageCount() <= 1) {
			new QOTWNotificationService(guild)
					.sendSubmissionActionNotification(guild.getJDA().getSelfUser(), thread, SubmissionStatus.DELETED, "Empty Submission");
			DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.deleteSubmission(thread.getIdLong()));
			thread.delete().queue();
			return;
		}
		thread.getManager().setName(String.format("%s %s", SUBMISSION_PENDING, thread.getName())).queue();
		thread.sendMessage(config.getQOTWReviewRole().getAsMention())
				.setEmbeds(new EmbedBuilder()
						.setTitle("Submission Controls")
						.setDescription("Please choose an action for this Submission.")
						.setTimestamp(Instant.now())
						.build())
				.setActionRow(
						Button.success("qotw-submission:controls:accept", "Accept"),
						Button.danger("qotw-submission:controls:decline", "Decline"),
						Button.secondary("qotw-submission:controls:delete", "🗑️")
				).queue();
		log.info("Sent Submission Controls to thread {}", thread.getName());
	}

	/**
	 * Accepts the current submission.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 */
	protected void acceptSubmission(ButtonInteractionEvent event, ThreadChannel thread) {
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.updateStatus(thread.getIdLong(), SubmissionStatus.ACCEPTED));
		thread.getManager().setName(SUBMISSION_ACCEPTED + thread.getName().substring(1)).queue();
		event.getJDA().retrieveUserById(submission.getAuthorId()).queue(user -> {
			QOTWPointsService service = new QOTWPointsService(Bot.dataSource);
			service.increment(user.getIdLong());
			new QOTWNotificationService(user, event.getGuild()).sendAccountIncrementedNotification();
			Responses.success(event.getHook(), "Submission Accepted",
					"Successfully accepted submission by " + user.getAsMention()).queue();
			}
		);
		this.disableControls(String.format("Accepted by %s", event.getUser().getAsTag()), event.getMessage());
		new QOTWNotificationService(guild).sendSubmissionActionNotification(event.getUser(), thread, SubmissionStatus.ACCEPTED);
	}

	/**
	 * Declines the current submission.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 */
	protected void declineSelectSubmission(SelectMenuInteractionEvent event, ThreadChannel thread) {
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.updateStatus(thread.getIdLong(), SubmissionStatus.DECLINED));
		thread.getManager().setName(SUBMISSION_DECLINED + thread.getName().substring(1)).queue();
		event.getJDA().retrieveUserById(submission.getAuthorId()).queue(user -> {
				new QOTWNotificationService(user, event.getGuild()).sendSubmissionDeclinedEmbed(String.join(", ", event.getValues()));
				Responses.success(event.getHook(), "Submission Declined",
						String.format("Successfully declined submission by %s for the following reasons:\n`%s`", user.getAsMention(), String.join(", ", event.getValues()))).queue();
				}
		);
		this.disableControls(String.format("Declined by %s", event.getUser().getAsTag()), event.getMessage());
		new QOTWNotificationService(guild).sendSubmissionActionNotification(event.getUser(), thread, SubmissionStatus.DECLINED, event.getValues().toArray(new String[0]));
	}

	/**
	 * Deletes the current submission.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 */
	protected void deleteSubmission(ButtonInteractionEvent event, ThreadChannel thread) {
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.deleteSubmission(thread.getIdLong()));
		event.getHook().sendMessage("This Submission will be deleted in 10 seconds.").setEphemeral(true).queue();
		this.disableControls(String.format("Deleted by %s", event.getUser().getAsTag()), event.getMessage());
		new QOTWNotificationService(guild).sendSubmissionActionNotification(event.getUser(), thread, SubmissionStatus.DELETED);
		thread.delete().queueAfter(10, TimeUnit.SECONDS);
	}

	private void disableControls(String buttonLabel, Message message) {
		message.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:controls:dummy", buttonLabel).asDisabled())).queue();
	}
}
