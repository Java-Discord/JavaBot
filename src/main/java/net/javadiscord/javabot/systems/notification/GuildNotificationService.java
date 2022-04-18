package net.javadiscord.javabot.systems.notification;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Sends notifications within a single {@link Guild}.
 */
public non-sealed class GuildNotificationService extends NotificationService {

	private final Guild guild;
	private final GuildConfig config;

	public GuildNotificationService(Guild guild) {
		this.guild = guild;
		this.config = Bot.config.get(guild);
	}

	public void sendLogChannelNotification(MessageEmbed embed) {
		this.sendMessageChannelNotification(config.getModeration().getLogChannel(), embed);
	}

	public void sendLogChannelNotification(String string, Object... args) {
		this.sendMessageChannelNotification(config.getModeration().getLogChannel(), string, args);
	}

	public void sendMessageLogChannelNotification(MessageEmbed embed) {
		this.sendMessageChannelNotification(config.getMessageCache().getMessageCacheLogChannel(), embed);
	}

	private void sendMessageLogChannelNotification(String string, Object... args) {
		this.sendMessageChannelNotification(config.getMessageCache().getMessageCacheLogChannel(), string, args);
	}
}
