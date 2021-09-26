package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

/**
 * A strategy to use to generate names for new help channels as they're needed.
 */
public interface ChannelNamingStrategy {
	String getName(List<TextChannel> channels, HelpConfig config);
}
