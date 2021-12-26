package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.guild.HelpConfig;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A naming strategy that names channels with a random animal name.
 */
public class AnimalNamingStrategy implements ChannelNamingStrategy {
	private static final String[] ANIMALS = {
			"bear", "tiger", "lion", "snake", "cheetah", "panther", "bat", "mosquito", "opossum", "raccoon", "beaver",
			"walrus", "seal", "dolphin", "shark", "narwhal", "orca", "whale", "squid", "tuna", "nautilus", "jellyfish",
			"seagull", "eagle", "hawk", "flamingo", "spoonbill", "puffin", "condor", "albatross", "parrot", "parakeet",
			"rabbit", "sloth", "deer", "boar", "ferret", "dog", "cat", "marmoset", "mole", "lizard", "kangaroo"
	};

	@Override
	public String getName(List<TextChannel> channels, HelpConfig config) {
		String name = ANIMALS[ThreadLocalRandom.current().nextInt(ANIMALS.length)];
		return "help-" + name;
	}
}
