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
	private String closeReminderText = "Hey, %s!\nPlease remember to `/close` this post once your question has been answered!";
	private String helpThanksText = "Before your post will be closed, would you like to express your gratitude to any of the people who helped you? When you're done, click **I'm done here. Close this post!**.";

	public ForumChannel getHelpForumChannel() {
		return getGuild().getForumChannelById(helpForumChannelId);
	}
}
