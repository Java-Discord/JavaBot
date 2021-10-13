package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StarBoardConfig extends GuildConfigItem {
	private long channelId;
	private int reactionThreshold;
	private List<String> emotes = new ArrayList<>();

	public TextChannel getChannel() {
		return this.getGuild().getTextChannelById(this.channelId);
	}
}
