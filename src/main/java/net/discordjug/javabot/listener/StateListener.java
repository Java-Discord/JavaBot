package net.discordjug.javabot.listener;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.annotations.PreRegisteredListener;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.systems.staff_commands.tags.CustomTagManager;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Listens for the {@link ReadyEvent}.
 */
@Slf4j
@RequiredArgsConstructor
@PreRegisteredListener
public class StateListener extends ListenerAdapter {
	private final NotificationService notificationService;
	private final CustomTagManager customTagManager;
	private final BotConfig botConfig;

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		// Initialize all guild-specific configuration.
		botConfig.loadGuilds(event.getJDA().getGuilds());
		botConfig.flush();
		log.info("Logged in as " + event.getJDA().getSelfUser().getAsTag());
		log.info("Guilds: " + event.getJDA().getGuilds().stream().map(Guild::getName).collect(Collectors.joining(", ")));
		for (Guild guild : event.getJDA().getGuilds()) {
			notificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageEmbeds(buildBootedUpEmbed()));
		}
		try {
			customTagManager.init(event.getJDA());
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			log.error("Could not initialize CustomCommandManager: ", e);
		}
	}

	@Override
	public void onSessionRecreate(@NotNull SessionRecreateEvent event) {
		botConfig.loadGuilds(event.getJDA().getGuilds());
		botConfig.flush();
	}

	@Override
	public void onShutdown(@NotNull ShutdownEvent event) {
		botConfig.flush();
	}

	private @NotNull MessageEmbed buildBootedUpEmbed() {
		return new EmbedBuilder()
				.setTitle("I've just been booted up!")
				.addField("Operating System", StringUtils.getOperatingSystem(), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
