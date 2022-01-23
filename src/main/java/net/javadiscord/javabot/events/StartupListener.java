package net.javadiscord.javabot.events;


import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.systems.help.HelpChannelUpdater;
import net.javadiscord.javabot.systems.help.checks.SimpleGreetingCheck;
import net.javadiscord.javabot.systems.starboard.StarboardManager;
import net.javadiscord.javabot.util.Misc;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StartupListener extends ListenerAdapter {

	public static Guild defaultGuild;

	@Override
	public void onReady(ReadyEvent event) {
		// Initialize all guild-specific configuration.
		Bot.config.loadGuilds(event.getJDA().getGuilds());
		Bot.config.flush();
		log.info("Logged in as {}{}{}", Constants.TEXT_WHITE, event.getJDA().getSelfUser().getAsTag(), Constants.TEXT_RESET);
		log.info("Guilds: " + Misc.getGuildList(event.getJDA().getGuilds(), true, true));
		var optionalGuild = event.getJDA().getGuilds().stream().max(Comparator.comparing(Guild::getMemberCount));
		optionalGuild.ifPresent(guild -> defaultGuild = guild);

		log.info("Starting Guild initialization\n");
		for (var guild : event.getJDA().getGuilds()) {
			Bot.slashCommands.registerSlashCommands(guild);
			new StarboardManager().updateAllStarboardEntries(guild);
			// Schedule the help channel updater to run periodically for each guild.
			var helpConfig = Bot.config.get(guild).getHelp();
			Bot.asyncPool.scheduleAtFixedRate(
					new HelpChannelUpdater(event.getJDA(), helpConfig, List.of(
							new SimpleGreetingCheck()
					)),
					5,
					helpConfig.getUpdateIntervalSeconds(),
					TimeUnit.SECONDS
			);
			Misc.sendToLog(guild, "I have just been booted up!");
		}
	}
}
