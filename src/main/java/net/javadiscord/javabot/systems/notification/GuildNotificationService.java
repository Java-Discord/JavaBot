package net.javadiscord.javabot.systems.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Handles all sorts of guild notifications.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class GuildNotificationService extends NotificationService.MessageChannelNotification {

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
