package net.javadiscord.javabot.tasks;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.MetricsConfig;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Periodically updates the Stats Categories for each guild in a set interval.
 * <p>
 * This updater should be added as an event listener to the bot, so that it
 * will automatically begin operation when the bot gives the ready event.
 * </p>
 */
@Slf4j
public class MetricsUpdater extends ListenerAdapter {
	private static final Map<String, Function<Guild, String>> TEXT_VARIABLES = Map.of(
			"{!member_count}", g -> String.valueOf(g.getMemberCount()),
			"{!server_name}", Guild::getName
	);

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		Bot.getAsyncPool().scheduleWithFixedDelay(() -> {
			for (Guild guild : event.getJDA().getGuilds()) {
				MetricsConfig config = Bot.getConfig().get(guild).getMetricsConfig();
				if (config.getMetricsCategory() == null || config.getMetricsMessageTemplate().isEmpty()) {
					continue;
				}
				String text = config.getMetricsMessageTemplate();
				for (Map.Entry<String, Function<Guild, String>> entry : TEXT_VARIABLES.entrySet()) {
					text = text.replace(entry.getKey(), entry.getValue().apply(guild));
				}
				config.getMetricsCategory().getManager().setName(text).queue(s -> log.info("Successfully updated Metrics"), t -> ExceptionLogger.capture(t, getClass().getSimpleName()));
			}
		}, 0, 20, TimeUnit.MINUTES);
	}
}
