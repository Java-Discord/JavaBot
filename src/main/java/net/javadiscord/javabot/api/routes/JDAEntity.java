package net.javadiscord.javabot.api.routes;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.Bot;

public interface JDAEntity {
	default JDA getJDA() {
		return Bot.getDih4jda().getJDA();
	}
}
