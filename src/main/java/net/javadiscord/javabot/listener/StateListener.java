package net.javadiscord.javabot.listener;


import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.systems.help.HelpChannelUpdater;
import net.javadiscord.javabot.systems.help.checks.SimpleGreetingCheck;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Listens for the {@link ReadyEvent}.
 */
@Slf4j
public class StateListener extends ListenerAdapter {
	@Override
	public void onReady(@NotNull ReadyEvent event) {
		// Initialize all guild-specific configuration.
		Bot.getConfig().loadGuilds(event.getJDA().getGuilds());
		Bot.getConfig().flush();
		log.info("Logged in as " + event.getJDA().getSelfUser().getAsTag());
		log.info("Guilds: " + event.getJDA().getGuilds().stream().map(Guild::getName).collect(Collectors.joining(", ")));
		for (Guild guild : event.getJDA().getGuilds()) {
			// Schedule the help channel updater to run periodically for each guild.
			HelpConfig helpConfig = Bot.getConfig().get(guild).getHelpConfig();
			Bot.getAsyncPool().scheduleAtFixedRate(
					new HelpChannelUpdater(event.getJDA(), helpConfig, List.of(
							new SimpleGreetingCheck()
					)),
					5,
					helpConfig.getUpdateIntervalSeconds(),
					TimeUnit.SECONDS
			);
			NotificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageEmbeds(buildBootedUpEmbed()));
		}
		try {
			Bot.getCustomTagManager().init();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			log.error("Could not initialize CustomCommandManager: ", e);
		}
	}

	@Override
	public void onReconnected(@NotNull ReconnectedEvent event) {
		Bot.getConfig().loadGuilds(event.getJDA().getGuilds());
		Bot.getConfig().flush();
	}

	@Override
	public void onShutdown(@NotNull ShutdownEvent event) {
		Bot.getConfig().flush();
	}

	private @NotNull MessageEmbed buildBootedUpEmbed() {
		return new EmbedBuilder()
				.setTitle("I've just been booted up!")
				.addField("Operating System", StringUtils.getOperatingSystem(), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
