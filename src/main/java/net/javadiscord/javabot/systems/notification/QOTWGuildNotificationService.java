package net.javadiscord.javabot.systems.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.javadiscord.javabot.systems.qotw.model.QOTWSubmission;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;

import net.javadiscord.javabot.util.UserUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

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

	/**
	 * Sends the executed action, performed on a QOTW submission thread, to the {@link Guild}s log channel.
	 *
	 * @param reviewedBy       The user which reviewed the QOTW submission thread.
	 * @param submission	   The {@link QOTWSubmission}.
	 * @param status           The {@link SubmissionStatus}.
	 */
	public void sendSubmissionActionNotification(User reviewedBy, @NotNull QOTWSubmission submission, SubmissionStatus status) {
		submission.retrieveAuthor(author -> {
			notificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageEmbeds(buildSubmissionActionEmbed(author, submission.getThread(), reviewedBy, status)));
			log.info("{} {} {}'s QOTW Submission", UserUtils.getUserTag(reviewedBy), status.getVerb(), UserUtils.getUserTag(author));
		});
	}

	private @NotNull MessageEmbed buildSubmissionActionEmbed(@NotNull User author, ThreadChannel thread, @NotNull User reviewedBy, @NotNull SubmissionStatus status) {
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(reviewedBy), null, reviewedBy.getEffectiveAvatarUrl())
				.setTitle(String.format("%s %s %s's QOTW Submission", UserUtils.getUserTag(reviewedBy), status.getVerb(), UserUtils.getUserTag(author)))
				.setTimestamp(Instant.now());
		if (thread != null) {
			builder.addField("Thread", thread.getAsMention(), true);
		}
		return builder.build();
	}
}
