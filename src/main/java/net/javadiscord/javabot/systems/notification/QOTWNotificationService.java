package net.javadiscord.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.data.config.SystemsConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.SQLException;
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

	QOTWNotificationService(NotificationService notificationService, QOTWPointsService pointsService,@NotNull User user, Guild guild, SystemsConfig systemsConfig, DbHelper dbHelper) {
		super(notificationService, guild, dbHelper);
		this.user = user;
		this.guild = guild;
		QOTWAccount account;
		try {
			account = pointsService.getOrCreateAccount(user.getIdLong());
		} catch (SQLException e) {
			log.error("Could not find Account with user Id: {}", user.getIdLong(), e);
			account = null;
		}
		this.account = account;
		this.systemsConfig = systemsConfig;
	}

	public void sendBestAnswerNotification() {
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildBestAnswerEmbed(account.getPoints())));
	}

	public void sendAccountIncrementedNotification() {
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildAccountIncrementEmbed(account.getPoints())));
	}

	public void sendSubmissionDeclinedEmbed(@Nonnull String reason) {
		notificationService.withUser(user).sendDirectMessage(c -> c.sendMessageEmbeds(buildSubmissionDeclinedEmbed(reason)));
	}

	private @NotNull EmbedBuilder buildQOTWNotificationEmbed() {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setTimestamp(Instant.now());
	}

	private @NotNull MessageEmbed buildBestAnswerEmbed(long points) {
		return buildQOTWNotificationEmbed()
				.setColor(Responses.Type.SUCCESS.getColor())
				.setDescription(String.format(
						"""
								Your submission was marked as the best answer!
								You've been granted **`1 extra QOTW-Point`**! (total: %s)""", points))
				.build();
	}

	private @NotNull MessageEmbed buildAccountIncrementEmbed(long points) {
		return buildQOTWNotificationEmbed()
				.setColor(Responses.Type.SUCCESS.getColor())
				.setDescription(String.format(
						"""
								Your submission was accepted! %s
								You've been granted **`1 QOTW-Point`**! (total: %s)""",
						systemsConfig.getEmojiConfig().getSuccessEmote(guild.getJDA()), points))
				.build();
	}

	private @NotNull MessageEmbed buildSubmissionDeclinedEmbed(String reasons) {
		return this.buildQOTWNotificationEmbed()
				.setColor(Responses.Type.ERROR.getColor())
				.setDescription(String.format("""
								Hey %s,
								Your QOTW-Submission was **declined** for the following reasons:
								**`%s`**

								However, you can try your luck again next week!""",
						user.getAsMention(), reasons))
				.build();
	}
}
