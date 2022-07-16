package net.javadiscord.javabot.tasks;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;

import java.util.concurrent.TimeUnit;

/**
 * Periodically updates the Stats Categories for each guild in a set interval.
 * <p>
 * This updater should be added as an event listener to the bot, so that it
 * will automatically begin operation when the bot gives the ready event.
 * </p>
 */
@Slf4j
public class MetricsUpdater extends ListenerAdapter {
	@Override
	public void onReady(ReadyEvent event) {
		Bot.asyncPool.scheduleWithFixedDelay(() -> {
			for (var guild : event.getJDA().getGuilds()) {
				var config = Bot.config.get(guild).getStats();
				if (
						config.getCategoryId() == 0 ||
								config.getMemberCountMessageTemplate() == null
				) {
					continue;
				}
				String text = config.getMemberCountMessageTemplate()
						.replace("{!membercount}", String.valueOf(guild.getMemberCount()))
						.replace("{!server}", guild.getName());
				config.getCategory().getManager().setName(text).queue();
			}
			log.info("Successfully updated Stats Categories");
		}, 0, 20, TimeUnit.MINUTES);
	}
}
