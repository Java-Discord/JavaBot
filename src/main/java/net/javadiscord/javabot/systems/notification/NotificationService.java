package net.javadiscord.javabot.systems.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Handles all types of guild & user notifications.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
	private final QOTWPointsService qotwPointsService;
	private final BotConfig botConfig;
	private final ExecutorService asyncPool;
	private final QOTWSubmissionRepository qotwSubmissionRepository;

	@Contract("_ -> new")
	public @NotNull GuildNotificationService withGuild(Guild guild) {
		return new GuildNotificationService(botConfig.get(guild));
	}

	@Contract("_ -> new")
	public @NotNull UserNotificationService withUser(User user) {
		return new UserNotificationService(user);
	}

	public @NotNull QOTWGuildNotificationService withQOTW(Guild guild) {
		return new QOTWGuildNotificationService(this, guild, asyncPool, qotwSubmissionRepository);
	}

	public @NotNull QOTWNotificationService withQOTW(Guild guild, User user) {
		return new QOTWNotificationService(this, qotwPointsService, user, guild, botConfig.getSystems(), asyncPool, qotwSubmissionRepository);
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
