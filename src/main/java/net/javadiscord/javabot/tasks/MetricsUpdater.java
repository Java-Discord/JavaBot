package net.javadiscord.javabot.tasks;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.StatsConfig;
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
		Bot.asyncPool.scheduleWithFixedDelay(() -> {
			for (Guild guild : event.getJDA().getGuilds()) {
				StatsConfig config = Bot.config.get(guild).getStats();
				if (config.getCategoryId() == 0 || config.getMemberCountMessageTemplate() == null) {
					continue;
				}
				String text = config.getMemberCountMessageTemplate();
				for (Map.Entry<String, Function<Guild, String>> entry : TEXT_VARIABLES.entrySet()) {
					text = text.replace(entry.getKey(), entry.getValue().apply(guild));
				}
				config.getCategory().getManager().setName(text).queue(s -> log.info("Successfully updated Metrics"), ExceptionLogger::capture);
			}
		}, 0, 20, TimeUnit.MINUTES);
	}
}
