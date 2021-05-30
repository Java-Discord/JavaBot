package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.events.Startup;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

public class Uptime extends Command {

    public static void exCommand (CommandEvent event) {

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeMS = rb.getUptime();

        long uptimeDAYS = TimeUnit.MILLISECONDS.toDays(uptimeMS);
        uptimeMS -= TimeUnit.DAYS.toMillis(uptimeDAYS);
        long uptimeHRS = TimeUnit.MILLISECONDS.toHours(uptimeMS);
        uptimeMS -= TimeUnit.HOURS.toMillis(uptimeHRS);
        long uptimeMIN = TimeUnit.MILLISECONDS.toMinutes(uptimeMS);
        uptimeMS -= TimeUnit.MINUTES.toMillis(uptimeMIN);
        long uptimeSEC = TimeUnit.MILLISECONDS.toSeconds(uptimeMS);

        String botImage = event.getJDA().getSelfUser().getAvatarUrl();
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(uptimeDAYS + "d " + uptimeHRS + "h " + uptimeMIN + "min " + uptimeSEC + "s", null, botImage)
                .setColor(new Color(0x2F3136));
        event.reply(eb.build());

    }

    public static void exCommand (SlashCommandEvent event) {

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeMS = rb.getUptime();

        long uptimeDAYS = TimeUnit.MILLISECONDS.toDays(uptimeMS);
        uptimeMS -= TimeUnit.DAYS.toMillis(uptimeDAYS);
        long uptimeHRS = TimeUnit.MILLISECONDS.toHours(uptimeMS);
        uptimeMS -= TimeUnit.HOURS.toMillis(uptimeHRS);
        long uptimeMIN = TimeUnit.MILLISECONDS.toMinutes(uptimeMS);
        uptimeMS -= TimeUnit.MINUTES.toMillis(uptimeMIN);
        long uptimeSEC = TimeUnit.MILLISECONDS.toSeconds(uptimeMS);

        String botImage = Startup.bot.getAvatarUrl();
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(uptimeDAYS + "d " + uptimeHRS + "h " + uptimeMIN + "min " + uptimeSEC + "s", null, botImage)
                .setColor(new Color(0x2F3136));
        event.replyEmbeds(eb.build()).queue();

    }

    public Uptime() {
        this.name = "uptime";
        this.category = new Category("USER COMMANDS");
        this.help = "Checks Java's uptime";
    }

    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}