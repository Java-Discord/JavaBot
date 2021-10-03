package com.javadiscord.javabot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StatsUpdater extends ListenerAdapter {

    private final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

    private final long delay = 20;

    private final TimeUnit delayUnit = TimeUnit.MINUTES;

    private JDA jda;

    @Override
    public void onReady(ReadyEvent event) {
        this.jda = event.getJDA();
        threadPool.scheduleWithFixedDelay(() -> {
            for (var guild : jda.getGuilds()) {
                var config = Bot.config.get(guild).getStats();
                if (
                        config.getCategoryId() == 0 ||
                        config.getMemberCountMessageTemplate() == null
                ) continue;

                String text = config.getMemberCountMessageTemplate()
                        .replace("{!membercount}", String.valueOf(guild.getMemberCount()))
                        .replace("{!server}", guild.getName());
                config.getStatsCategory().getManager().setName(text).queue();
            }
            log.info("Successfully updated Stats Categories");
        }, 0, this.delay, this.delayUnit);
    }
}
