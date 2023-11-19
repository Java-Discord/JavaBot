package net.discordjug.javabot.systems.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.GuildConfig;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Handles all sorts of guild notifications.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class GuildNotificationService extends NotificationService.MessageChannelNotification {

	private final GuildConfig guildConfig;

	/**
	 * Sends a notification to the log channel.
	 *
	 * @param function The {@link Function} to use which MUST return a {@link MessageCreateAction}.
	 */
	public void sendToModerationLog(@NotNull Function<MessageChannel, MessageCreateAction> function) {
		MessageChannel channel = guildConfig.getModerationConfig().getLogChannel();
		if (channel == null) {
			log.error("Could not send message to LogChannel in guild " + guildConfig.getGuild().getId());
			return;
		}
		send(channel, function);
	}

	/**
	 * Sends a notification to the message cache log channel.
	 *
	 * @param function The {@link Function} to use which MUST return a {@link MessageCreateAction}.
	 */
	public void sendToMessageLog(@NotNull Function<MessageChannel, MessageCreateAction> function) {
		MessageChannel channel = guildConfig.getMessageCacheConfig().getMessageCacheLogChannel();
		if (channel == null) {
			log.error("Could not find MessageCacheLogChannel in guild " + guildConfig.getGuild().getId());
			return;
		}
		send(channel, function);
	}
}
