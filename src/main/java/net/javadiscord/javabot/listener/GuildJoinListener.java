package net.javadiscord.javabot.listener;

import com.dynxsty.dih4jda.DIH4JDA;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.ExceptionLogger;

/**
 * Listens for {@link GuildJoinEvent}.
 */
@RequiredArgsConstructor
public class GuildJoinListener extends ListenerAdapter {
	private final BotConfig botConfig;
	private final DIH4JDA dih4jda;

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		botConfig.addGuild(event.getGuild());
		try {
			dih4jda.registerInteractions();
		} catch (ReflectiveOperationException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
	}
}
