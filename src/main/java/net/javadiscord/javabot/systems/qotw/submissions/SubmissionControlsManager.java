package net.javadiscord.javabot.systems.qotw.submissions;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.Responses;

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
	private QOTWSubmission submission;
	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final DbHelper dbHelper;

	/**
	 * The constructor of this class.
	 *
	 * @param guildConfig         Configuration of the guild where submission controls should be managed.
	 * @param submission          The {@link QOTWSubmission}.
	 * @param pointsService       The {@link QOTWPointsService}
	 * @param notificationService The {@link NotificationService}
	 * @param dbHelper            An object managing databse operations
	 */
	public SubmissionControlsManager(GuildConfig guildConfig, DbHelper dbHelper, QOTWSubmission submission, QOTWPointsService pointsService, NotificationService notificationService) {
		this.guild = guildConfig.getGuild();
		this.submission = submission;
		this.config = guildConfig.getQotwConfig();
		this.pointsService = pointsService;
		this.notificationService = notificationService;
		this.dbHelper=dbHelper;
	}

	/**
	 * The constructor of this class.
	 *
	 * @param guildConfig         Configuration of the guild where submission controls should be managed.
	 * @param channel             The {@link ThreadChannel}, which is used to retrieve the corresponding {@link QOTWSubmission}.
	 * @param pointsService       The {@link QOTWPointsService}
	 * @param notificationService The {@link NotificationService}
	 * @param dbHelper            An object managing databse operations
	 */
	public SubmissionControlsManager(GuildConfig guildConfig, DbHelper dbHelper, ThreadChannel channel, QOTWPointsService pointsService, NotificationService notificationService) {
		dbHelper.doDaoAction(QOTWSubmissionRepository::new, repo->{
			Optional<QOTWSubmission> submissionOptional = repo.getSubmissionByThreadId(channel.getIdLong());
			if (submissionOptional.isEmpty()) {
				log.error("Could not retrieve Submission from Thread: " + channel.getId());
			} else {
				submission = submissionOptional.get();
			}
		});
		this.guild = guildConfig.getGuild();
		this.config = guildConfig.getQotwConfig();
		this.pointsService = pointsService;
		this.notificationService = notificationService;
		this.dbHelper=dbHelper;
	}

	/**
	 * Sends an embed in the submission's thread channel that allows QOTW-Reviewers to perform various actions.
	 */
	public void sendControls() {
		ThreadChannel thread = this.guild.getThreadChannelById(this.submission.getThreadId());
		if (thread == null) return;
		// The Thread's starting message
		if (thread.getMessageCount() <= 1) {
			thread.sendMessage("This submission would've been deleted as no messages were detected.").queue();
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
		dbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.updateStatus(thread.getIdLong(), SubmissionStatus.ACCEPTED));
		thread.getManager().setName(SUBMISSION_ACCEPTED + thread.getName().substring(1)).queue();
		event.getJDA().retrieveUserById(submission.getAuthorId()).queue(user -> {
			pointsService.increment(user.getIdLong());
			notificationService.withQOTW(event.getGuild(), user).sendAccountIncrementedNotification();
			Responses.success(event.getHook(), "Submission Accepted",
					"Successfully accepted submission by " + user.getAsMention()).queue();
			}
		);
		this.disableControls(String.format("Accepted by %s", event.getUser().getAsTag()), event.getMessage());
		notificationService.withQOTW(guild).sendSubmissionActionNotification(event.getUser(), thread, SubmissionStatus.ACCEPTED);
	}

	/**
	 * Declines the current submission.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 */
	protected void declineSelectSubmission(SelectMenuInteractionEvent event, ThreadChannel thread) {
		dbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.updateStatus(thread.getIdLong(), SubmissionStatus.DECLINED));
		thread.getManager().setName(SUBMISSION_DECLINED + thread.getName().substring(1)).queue();
		event.getJDA().retrieveUserById(submission.getAuthorId()).queue(user -> {
			notificationService.withQOTW(event.getGuild(), user).sendSubmissionDeclinedEmbed(String.join(", ", event.getValues()));
				Responses.success(event.getHook(), "Submission Declined",
						String.format("Successfully declined submission by %s for the following reasons:\n`%s`", user.getAsMention(), String.join(", ", event.getValues()))).queue();
				}
		);
		this.disableControls(String.format("Declined by %s", event.getUser().getAsTag()), event.getMessage());
		notificationService.withQOTW(guild).sendSubmissionActionNotification(event.getUser(), thread, SubmissionStatus.DECLINED, event.getValues().toArray(new String[0]));
	}

	/**
	 * Deletes the current submission.
	 *
	 * @param event The {@link ButtonInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 */
	protected void deleteSubmission(ButtonInteractionEvent event, ThreadChannel thread) {
		dbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> dao.deleteSubmission(thread.getIdLong()));
		event.getHook().sendMessage("This Submission will be deleted in 10 seconds.").setEphemeral(true).queue();
		this.disableControls(String.format("Deleted by %s", event.getUser().getAsTag()), event.getMessage());
		notificationService.withQOTW(guild).sendSubmissionActionNotification(event.getUser(), thread, SubmissionStatus.DELETED);
		thread.delete().queueAfter(10, TimeUnit.SECONDS);
	}

	private void disableControls(String buttonLabel, Message message) {
		message.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:controls:dummy", buttonLabel).asDisabled())).queue();
	}
}
