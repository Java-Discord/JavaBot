package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.javadiscord.javabot.data.config.GuildConfigItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the guild's starboard system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StarboardConfig extends GuildConfigItem {
	private long starboardChannelId;
	private int reactionThreshold;
	private List<String> emojiUnicodes = new ArrayList<>();

	public TextChannel getStarboardChannel() {
		return this.getGuild().getTextChannelById(this.starboardChannelId);
	}

	public List<UnicodeEmoji> getEmojis() {
		return emojiUnicodes.stream().map(Emoji::fromUnicode).toList();
	}
}
