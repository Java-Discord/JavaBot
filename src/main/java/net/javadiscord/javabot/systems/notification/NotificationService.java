package net.javadiscord.javabot.systems.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Handles all types of guild & user notifications.
 */
public final class NotificationService {
	private NotificationService() {}

	@Contract("_ -> new")
	public static @NotNull GuildNotificationService of(Guild guild) {
		return new GuildNotificationService(guild);
	}

	@Contract("_ -> new")
	public static @NotNull UserNotificationService of(User user) {
		return new UserNotificationService(user);
	}

	/**
	 * Handles all sorts of guild notifications.
	 */
	@Slf4j
	@RequiredArgsConstructor
	public static final class GuildNotificationService extends MessageChannelNotification {
		private final Guild guild;

		/**
		 * Sends a notification to the log channel.
		 *
		 * @param function The {@link Function} to use which MUST return a {@link MessageAction}.
		 */
		public void sendToModerationLog(@NotNull Function<MessageChannel, MessageAction> function) {
			MessageChannel channel = Bot.getConfig().get(guild).getModerationConfig().getLogChannel();
			if (channel == null) {
				log.error("Could not send message to LogChannel in guild " + guild.getId());
				return;
			}
			send(channel, function);
		}

		/**
		 * Sends a notification to the message cache log channel.
		 *
		 * @param function The {@link Function} to use which MUST return a {@link MessageAction}.
		 */
		public void sendToMessageLog(@NotNull Function<MessageChannel, MessageAction> function) {
			MessageChannel channel = Bot.getConfig().get(guild).getMessageCacheConfig().getMessageCacheLogChannel();
			if (channel == null) {
				log.error("Could not find MessageCacheLogChannel in guild " + guild.getId());
				return;
			}
			send(channel, function);
		}
	}

	/**
	 * Handles all sorts of user notifications.
	 */
	@Slf4j
	@RequiredArgsConstructor
	public static final class UserNotificationService extends MessageChannelNotification {
		private final User user;

		/**
		 * Sends a notification to a {@link User}s' {@link net.dv8tion.jda.api.entities.PrivateChannel}.
		 *
		 * @param function The {@link Function} to use which MUST return a {@link MessageAction}.
		 */
		public void sendDirectMessage(@NotNull Function<MessageChannel, MessageAction> function) {
			user.openPrivateChannel().queue(
					channel -> send(channel, function),
					error -> log.error("Could not open PrivateChannel with user " + user.getAsTag(), error)
			);
		}
	}

	@Slf4j
	private abstract static class MessageChannelNotification {
		protected void send(MessageChannel channel, @NotNull Function<MessageChannel, MessageAction> function) {
			function.apply(channel).queue(s -> {}, err -> {
				ExceptionLogger.capture(err, getClass().getSimpleName());
				log.error("Could not send message to channel \" " + channel.getName() + "\": ", err);
			});
		}
	}
}
