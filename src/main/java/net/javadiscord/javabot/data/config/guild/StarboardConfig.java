package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the guild's starboard system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StarboardConfig extends GuildConfigItem {
	private long channelId;
	private int reactionThreshold;
	private List<String> emotes = new ArrayList<>();

	public TextChannel getStarboardChannel() {
		return this.getGuild().getTextChannelById(this.channelId);
	}
}
