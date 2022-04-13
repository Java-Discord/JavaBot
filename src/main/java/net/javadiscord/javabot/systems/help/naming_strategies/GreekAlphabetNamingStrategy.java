package net.javadiscord.javabot.systems.help.naming_strategies;

import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.guild.HelpConfig;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Naming strategy that names help channels with a random letter. Note that the
 * chance for duplicates is quite high!
 */
public class GreekAlphabetNamingStrategy implements ChannelNamingStrategy {
	private static final String ALPHABET = "αβγδεζηθικλμνξοπρστυφχψω.";

	@Override
	public String getName(List<TextChannel> channels, HelpConfig config) {
		return "help-" + ALPHABET.charAt(ThreadLocalRandom.current().nextInt(ALPHABET.length()));
	}
}
