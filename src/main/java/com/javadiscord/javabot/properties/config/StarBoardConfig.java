package com.javadiscord.javabot.properties.config;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;

@Data
public class StarBoardConfig {
	private long channelId;
	private List<String> emotes = new ArrayList<>();

	public TextChannel getChannel(Guild guild) {
		return guild.getTextChannelById(this.channelId);
	}
}
