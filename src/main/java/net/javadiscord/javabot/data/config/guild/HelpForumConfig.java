package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

/**
 * Configuration for the guilds' forum help system, which aims to replace the
 * 'old' text channel-based one.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HelpForumConfig extends GuildConfigItem {
	private long helpForumChannelId = 0;

	public ForumChannel getHelpForumChannel() {
		return getGuild().getForumChannelById(helpForumChannelId);
	}
}
