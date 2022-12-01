package net.javadiscord.javabot.listener;

import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.DIH4JDA;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;

/**
 * Listens for {@link GuildJoinEvent}.
 */
@RequiredArgsConstructor
public class GuildJoinListener extends ListenerAdapter {
	private final BotConfig botConfig;
	private final DIH4JDA dih4jda;

	@Override
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		botConfig.addGuild(event.getGuild());
		dih4jda.registerInteractions();
	}
}
