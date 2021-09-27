package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Naming strategy that names help channels with a random letter. Note that the
 * chance for duplicates is quite high!
 */
public class AlphabetNamingStrategy implements ChannelNamingStrategy {
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	@Override
	public String getName(List<TextChannel> channels, HelpConfig config) {
		return config.getOpenChannelPrefix() + "help-" + ALPHABET.charAt(ThreadLocalRandom.current().nextInt(ALPHABET.length()));
	}
}
