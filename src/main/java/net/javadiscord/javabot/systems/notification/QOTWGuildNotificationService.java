package net.javadiscord.javabot.systems.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.ExceptionLogger;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Handles all sorts of guild qotw notifications.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class QOTWGuildNotificationService {
	/**
	 * The {@link NotificationService}.
	 */
	protected final NotificationService notificationService;
	private final Guild guild;
	private final ExecutorService asyncPool;
	private final QOTWSubmissionRepository qotwSubmissionRepository;

	/**
	 * Sends the executed action, performed on a QOTW submission thread, to the {@link Guild}s log channel.
	 *
	 * @param reviewedBy       The user which reviewed the QOTW submission thread.
	 * @param submissionThread The submission thread itself.
	 * @param status           The {@link SubmissionStatus}.
	 * @param reasons          The reasons for taking this action.
	 */
	public void sendSubmissionActionNotification(User reviewedBy, ThreadChannel submissionThread, SubmissionStatus status, @Nullable String... reasons) {
		asyncPool.execute(()->{
			try {
				Optional<QOTWSubmission> submissionOptional = qotwSubmissionRepository.getSubmissionByThreadId(submissionThread.getIdLong());
				submissionOptional.ifPresent(submission -> guild.getJDA().retrieveUserById(submission.getAuthorId()).queue(author -> {
					notificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageEmbeds(buildSubmissionActionEmbed(author, submissionThread, reviewedBy, status, reasons)));
					log.info("{} {} {}'s QOTW Submission{}", reviewedBy.getAsTag(), status.name().toLowerCase(), author.getAsTag(), reasons != null ? " for: " + String.join(", ", reasons) : ".");
				}));
			}catch (DataAccessException e) {
				ExceptionLogger.capture(e, QOTWGuildNotificationService.class.getSimpleName());
			}
		});
	}

	private @NotNull MessageEmbed buildSubmissionActionEmbed(@NotNull User author, ThreadChannel thread, @NotNull User reviewedBy, @NotNull SubmissionStatus status, String... reasons) {
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(reviewedBy.getAsTag(), null, reviewedBy.getEffectiveAvatarUrl())
				.setTitle(String.format("%s %s %s's QOTW Submission", reviewedBy.getAsTag(), status.name().toLowerCase(), author.getAsTag()))
				.setTimestamp(Instant.now());
		if (thread != null && status != SubmissionStatus.DELETED) {
			builder.addField("Thread", thread.getAsMention(), true);
		}
		if (reasons != null && reasons.length > 0) {
			builder.addField("Reason(s)", String.join(", ", reasons), true);
		}
		return builder.build();
	}
}
