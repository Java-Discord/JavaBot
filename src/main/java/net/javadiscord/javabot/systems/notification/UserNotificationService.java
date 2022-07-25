package net.javadiscord.javabot.systems.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

/**
 * Sends notifications within a single {@link net.dv8tion.jda.api.entities.Guild}.
 */
@Slf4j
@RequiredArgsConstructor
public final class UserNotificationService extends NotificationService {
	private final User user;

	/**
	 * Sends a {@link MessageEmbed} to the users {@link net.dv8tion.jda.api.entities.PrivateChannel}.
	 *
	 * @param embed The {@link MessageEmbed} to send.
	 */
	public void sendDirectMessageNotification(MessageEmbed embed) {
		sendDirectMessageNotification(user, embed);
	}

	/**
	 * Sends a simple Message to the users {@link net.dv8tion.jda.api.entities.PrivateChannel}.
	 *
	 * @param string The message that should be sent.
	 * @param args Optional args for formatting.
	 */
	public void sendDirectMessageNotification(String string, Object... args) {
		this.sendDirectMessageNotification(user, string, args);
	}
}
