package net.discordjug.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;
import net.discordjug.javabot.systems.qotw.submissions.SubmissionStatus;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.time.Instant;

/**
 * An extension of {@link QOTWGuildNotificationService} which also handles user qotw
 * notifications.
 */
@Slf4j
public final class QOTWNotificationService extends QOTWGuildNotificationService {
	private final Guild guild;
	private final User user;
	private final QOTWAccount account;
	private final SystemsConfig systemsConfig;

	QOTWNotificationService(NotificationService notificationService, QOTWPointsService pointsService,@NotNull User user, Guild guild, SystemsConfig systemsConfig) {
		super(notificationService, guild);
		this.user = user;
		this.guild = guild;
		QOTWAccount account;
		try {
			account = pointsService.getOrCreateAccount(user.getIdLong());
		} catch (DataAccessException e) {
			log.error("Could not find Account with user Id: {}", user.getIdLong(), e);
			account = null;
		}
		this.account = account;
		this.systemsConfig = systemsConfig;
	}

	public void sendBestAnswerNotification() {
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildBestAnswerEmbed(account.getPoints())));
	}

	public void sendAccountIncrementedNotification(ForumChannel acceptedAnswerForumChannel) {
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildAccountIncrementEmbed(account.getPoints(), acceptedAnswerForumChannel)));
	}

	public void sendAccountDecrementedNotification() {
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildAccountDecrementEmbed(account.getPoints())));
	}

	/**
	 * Sends a "your submission was declined"-notification to the {@link QOTWNotificationService#user}.
	 *
	 * @param status The {@link SubmissionStatus}. Must NOT be {@link SubmissionStatus#ACCEPT} or {@link SubmissionStatus#ACCEPT_BEST}
	 * @param acceptedAnswerForumChannel The forum channel containing accepted answers to the QOTW
	 */
	public void sendSubmissionDeclinedEmbed(SubmissionStatus status, ForumChannel acceptedAnswerForumChannel) {
		String reason = switch (status) {
			case DECLINE_WRONG_ANSWER -> "Wrong answer";
			case DECLINE_TOO_SHORT -> "Too short";
			case DECLINE_EMPTY -> "Empty submission";
			case DECLINE_PLAGIARISM -> "Plagiarism / AI usage";
			case ACCEPT, ACCEPT_BEST -> throw new IllegalArgumentException("Invalid decline status: " + status);
		};
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildSubmissionDeclinedEmbed(reason, acceptedAnswerForumChannel)));
	}

	private @NotNull EmbedBuilder buildQOTWNotificationEmbed() {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(user), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setTimestamp(Instant.now());
	}

	private @NotNull MessageEmbed buildBestAnswerEmbed(long points) {
		return buildQOTWNotificationEmbed()
				.setColor(Responses.Type.SUCCESS.getColor())
				.setDescription(String.format(
						"""
								Your submission was marked as the best answer!
								You've been granted **`1 extra QOTW-Point`**! (monthly total: %s)""", points))
				.build();
	}

	private @NotNull MessageEmbed buildAccountIncrementEmbed(long points, ForumChannel acceptedAnswerForumChannel) {
		return buildQOTWNotificationEmbed()
				.setColor(Responses.Type.SUCCESS.getColor())
				.setDescription(String.format(
						"""
								Your submission was accepted! %s
								You can view accepted answers in %s.
								You've been granted **`1 QOTW-Point`**! (monthly total: %s)""",
						systemsConfig.getEmojiConfig().getSuccessEmote(guild.getJDA()), acceptedAnswerForumChannel.getJumpUrl(), points))
				.build();
	}

	private @NotNull MessageEmbed buildAccountDecrementEmbed(long points) {
		return buildQOTWNotificationEmbed()
				.setColor(Responses.Type.WARN.getColor())
				.setDescription(String.format(
						"""
								Your QOTW score has been decremented.
								**`1 QOTW-Point`** has been deducted from your score! (monthly total: %s)
								In case you want to know why exactly this happened, please contact our staff team.""", points))
				.build();
	}

	private @NotNull MessageEmbed buildSubmissionDeclinedEmbed(String reason, ForumChannel acceptedAnswerForumChannel) {
		return this.buildQOTWNotificationEmbed()
				.setColor(Responses.Type.ERROR.getColor())
				.setDescription(String.format("""
								Hey %s,
								Your QOTW-Submission was **declined** for the following reason: `%s`.
								You can view accepted answers in %s.
								However, you can try your luck again next week!""",
						user.getAsMention(), reason, acceptedAnswerForumChannel.getJumpUrl()))
				.build();
	}

}
