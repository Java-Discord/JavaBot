package net.javadiscord.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Handles all types of guild & user notifications.
 */
public final class NotificationService {
	private NotificationService() {
	}

	@Contract("_ -> new")
	public static @NotNull GuildNotificationService withGuild(Guild guild) {
		return new GuildNotificationService(guild);
	}

	@Contract("_ -> new")
	public static @NotNull UserNotificationService withUser(User user) {
		return new UserNotificationService(user);
	}

	public static @NotNull QOTWGuildNotificationService withQOTW(Guild guild) {
		return new QOTWGuildNotificationService(guild);
	}

	public static @NotNull QOTWNotificationService withQOTW(Guild guild, User user) {
		return new QOTWNotificationService(user, guild);
	}

	/**
	 * Abstract class which streamlines the logic of sending messages to a {@link MessageChannel}.
	 */
	@Slf4j
	abstract static class MessageChannelNotification {
		/**
		 * Sends a single message to the specified {@link MessageChannel} using the
		 * specified {@link Function}.
		 *
		 * @param channel  The target {@link MessageChannel}.
		 * @param function The {@link Function} which is used in order to send the message.
		 */
		protected void send(MessageChannel channel, @NotNull Function<MessageChannel, MessageAction> function) {
			function.apply(channel).queue(s -> {
			}, err -> {
				ExceptionLogger.capture(err, getClass().getSimpleName());
				log.error("Could not send message to channel \" " + channel.getName() + "\": ", err);
			});
		}
	}
}
