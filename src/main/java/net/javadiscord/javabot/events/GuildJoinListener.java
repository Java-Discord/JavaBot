package net.javadiscord.javabot.events;

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
		for (var guild : event.getJDA().getGuilds()) {
			Bot.slashCommands.registerSlashCommands(guild);
		}
	}
}
