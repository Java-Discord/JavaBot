package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A naming strategy that names channels with a random coffee (bean) type.
 */
public class CoffeeNamingStrategy implements ChannelNamingStrategy {
	private static final String[] COFFEE = {
			"arabica", "espresso", "latte", "cappuccino", "americano", "doppio", "cortado", "lungo", "macchiato", "mocha", "ristretto",
			"affogato", "irish", "mazagran", "frappuccino", "nitro", "robusta", "liberica", "excelsa"
	};

	@Override
	public String getName(List<TextChannel> channels, HelpConfig config) {
		String name = COFFEE[ThreadLocalRandom.current().nextInt(COFFEE.length)];
		return "help-" + name;
	}
}
