package net.javadiscord.javabot.listener;

import io.sentry.Sentry;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;

/**
 * Listens for {@link GuildJoinEvent}.
 */
public class GuildJoinListener extends ListenerAdapter {
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		Bot.config.addGuild(event.getGuild());
		try {
			Bot.dih4jda.registerInteractions();
		} catch (ReflectiveOperationException e) {
			Sentry.captureException(e);
		}
	}
}
