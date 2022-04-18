package net.javadiscord.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Sends Notifications regarding QOTW.
 */
@Slf4j
public non-sealed class QOTWNotificationService extends NotificationService {

	private final User user;
	private final Guild guild;
	private final QOTWAccount account;

	/**
	 * The constructor of this class.
	 *
	 * @param user  The user that should be notified.
	 * @param guild The guild from where the notification was sent.
	 */
	public QOTWNotificationService(User user, Guild guild) {
		this.user = user;
		this.guild = guild;
		QOTWAccount account;
		try (Connection connection = Bot.dataSource.getConnection()) {
			QuestionPointsRepository dao = new QuestionPointsRepository(connection);
			account = dao.getAccountByUserId(user.getIdLong());
		} catch (SQLException e) {
			e.printStackTrace();
			account = null;
		}
		this.account = account;
	}

	public void sendBestAnswerNotification() {
		this.sendDirectMessageNotification(user, this.buildBestAnswerEmbed(account.getPoints()));
	}

	public void sendAccountIncrementedNotification() {
		this.sendDirectMessageNotification(user, this.buildAccountIncrementEmbed(account.getPoints()));
	}

	private MessageEmbed buildBestAnswerEmbed(long points) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was marked as the best answer!
								You've been granted **`1 extra QOTW-Point`**! (total: %s)""", points))
				.setTimestamp(Instant.now())
				.build();
	}

	private MessageEmbed buildAccountIncrementEmbed(long points) {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was accepted! %s
								You've been granted **`1 QOTW-Point`**! (total: %s)""",
						Bot.config.get(guild).getEmote().getSuccessEmote().getAsMention(), points))
				.setTimestamp(Instant.now())
				.build();
	}
}
