package net.javadiscord.javabot.api.routes;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.Bot;

/**
 * Simple interface which adds the {@link JDAEntity#getJDA()} method which
 * returns the bots' {@link JDA} client.
 */
public interface JDAEntity {
	default JDA getJDA() {
		return Bot.getDih4jda().getJDA();
	}
}
