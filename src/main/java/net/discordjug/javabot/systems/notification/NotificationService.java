package net.discordjug.javabot.systems.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * Handles all types of guild & user notifications.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
	private final QOTWPointsService qotwPointsService;
	private final BotConfig botConfig;

	@Contract("_ -> new")
	public @NotNull GuildNotificationService withGuild(Guild guild) {
		return new GuildNotificationService(botConfig.get(guild));
	}

	@Contract("_ -> new")
	public @NotNull UserNotificationService withUser(User user) {
		return new UserNotificationService(user);
	}

	@Contract("_ -> new")
	public @NotNull UserNotificationService withUser(User user, Guild guild) {
		return new UserNotificationService(user, botConfig.get(guild).getModerationConfig());
	}

	public @NotNull QOTWGuildNotificationService withQOTW(Guild guild) {
		return new QOTWGuildNotificationService(this, guild);
	}

	public @NotNull QOTWNotificationService withQOTW(Guild guild, User user) {
		return new QOTWNotificationService(this, qotwPointsService, user, guild, botConfig.getSystems());
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
		protected void send(MessageChannel channel, @NotNull Function<MessageChannel, MessageCreateAction> function) {
			function.apply(channel).queue(s -> {},
					err -> log.error("Could not send message to channel \" " + channel.getName() + "\": ", err)
			);
		}
	}
}
