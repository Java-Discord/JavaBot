package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

public class Uptime implements SlashCommandHandler {

    public String getUptime() {

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeMS = rb.getUptime();

        long uptimeDAYS = TimeUnit.MILLISECONDS.toDays(uptimeMS);
        uptimeMS -= TimeUnit.DAYS.toMillis(uptimeDAYS);
        long uptimeHRS = TimeUnit.MILLISECONDS.toHours(uptimeMS);
        uptimeMS -= TimeUnit.HOURS.toMillis(uptimeHRS);
        long uptimeMIN = TimeUnit.MILLISECONDS.toMinutes(uptimeMS);
        uptimeMS -= TimeUnit.MINUTES.toMillis(uptimeMIN);
        long uptimeSEC = TimeUnit.MILLISECONDS.toSeconds(uptimeMS);

        return String.format("%sd %sh %smin %ss",
                uptimeDAYS, uptimeHRS, uptimeMIN, uptimeSEC);
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String botImage = event.getJDA().getSelfUser().getAvatarUrl();
        var e = new EmbedBuilder()
            .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
            .setAuthor(getUptime(), null, botImage);

        return event.replyEmbeds(e.build());
    }
}