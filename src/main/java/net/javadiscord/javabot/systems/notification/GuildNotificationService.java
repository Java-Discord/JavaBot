package net.javadiscord.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Sends notifications within a single {@link Guild}.
 */
@Slf4j
public final class GuildNotificationService extends NotificationServiceDEPRECATED {

	private final Guild guild;
	private final GuildConfig config;

	public GuildNotificationService(Guild guild) {
		this.guild = guild;
		this.config = Bot.getConfig().get(guild);
	}

	/**
	 * Sends a {@link MessageEmbed} to the Guild's log channel.
	 *
	 * @param embed The {@link MessageEmbed} to send.
	 */
	public void sendLogChannelNotification(MessageEmbed embed) {
		if (config.getModerationConfig().getLogChannel() == null) {
			log.warn("Could not find Log Channel for Guild {}", guild.getName());
			return;
		}
		this.sendMessageChannelNotification(config.getModerationConfig().getLogChannel(), embed);
	}

	/**
	 * Sends a simple Message to the Guild's log channel.
	 *
	 * @param string The message that should be sent.
	 * @param args Optional args for formatting.
	 */
	public void sendLogChannelNotification(String string, Object... args) {
		if (config.getModerationConfig().getLogChannel() == null) {
			log.warn("Could not find Log Channel for Guild {}", guild.getName());
			return;
		}
		this.sendMessageChannelNotification(config.getModerationConfig().getLogChannel(), string, args);
	}

	/**
	 * Sends a {@link MessageEmbed} to the Guild's message log channel.
	 *
	 * @param embed The {@link MessageEmbed} to send.
	 */
	public void sendMessageLogChannelNotification(MessageEmbed embed) {
		if (config.getMessageCacheConfig().getMessageCacheLogChannel() == null) {
			log.warn("Could not find Message Log Channel for Guild {}", guild.getName());
			return;
		}
		this.sendMessageChannelNotification(config.getMessageCacheConfig().getMessageCacheLogChannel(), embed);
	}
}
