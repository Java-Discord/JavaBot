package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;

/**
 * Listens for {@link GuildJoinEvent}.
 */
public class GuildJoinListener extends ListenerAdapter {
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		Bot.getConfig().addGuild(event.getGuild());
		try {
			Bot.getDIH4JDA().registerInteractions();
		} catch (ReflectiveOperationException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
	}
}
