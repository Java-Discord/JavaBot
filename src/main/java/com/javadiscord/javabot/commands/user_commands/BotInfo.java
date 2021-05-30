package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.other.Version;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.javadiscord.javabot.events.Startup.bot;

public class BotInfo extends Command {

    public static void exCommand (CommandEvent event) {

        long ping = event.getJDA().getGatewayPing();
        String botImage = bot.getAvatarUrl();
        String botTag = bot.getAsTag();
        String botName = bot.getName();
        String botOS = System.getProperty("os.name");

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeMS = rb.getUptime();

        long uptimeDAYS = TimeUnit.MILLISECONDS.toDays(uptimeMS);
        uptimeMS -= TimeUnit.DAYS.toMillis(uptimeDAYS);
        long uptimeHRS = TimeUnit.MILLISECONDS.toHours(uptimeMS);
        uptimeMS -= TimeUnit.HOURS.toMillis(uptimeHRS);
        long uptimeMIN = TimeUnit.MILLISECONDS.toMinutes(uptimeMS);
        uptimeMS -= TimeUnit.MINUTES.toMillis(uptimeMIN);
        long uptimeSEC = TimeUnit.MILLISECONDS.toSeconds(uptimeMS);

        long botMemTotal = Runtime.getRuntime().totalMemory() / 1048576;
        long botMemFree = Runtime.getRuntime().freeMemory() / 1048576;
        long botMemUsed = botMemTotal - botMemFree;

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(0x2F3136))
                .setDescription("**" + botName + "** - currently running Version ``" + new Version().getVersion() + "``")
                .setThumbnail(botImage)
                .setAuthor(botName + " | Info", null, botImage)
                .addField("Name", "```" + botTag + "```", true)
                .addField("OS", "```" + botOS + "```", true)
                .addField("Library", "```JDA (Java)```", true)
                .addField("Ping", "```" + ping + "ms```", true)
                .addField("Uptime", "```" + uptimeDAYS + "d " + uptimeHRS + "h " + uptimeMIN + "min " + uptimeSEC + "s```", true)
                .addField("Memory (current Runtime)", "```" + botMemUsed + "MB / " + botMemTotal + "MB```", false)
                .setTimestamp(new Date().toInstant());

        event.reply(eb.build());
    }

    public static void exCommand (SlashCommandEvent event) {

        long ping = event.getJDA().getGatewayPing();
        String botImage = bot.getAvatarUrl();
        String botTag = bot.getAsTag();
        String botName = bot.getName();
        String botOS = System.getProperty("os.name");

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeMS = rb.getUptime();

        long uptimeDAYS = TimeUnit.MILLISECONDS.toDays(uptimeMS);
        uptimeMS -= TimeUnit.DAYS.toMillis(uptimeDAYS);
        long uptimeHRS = TimeUnit.MILLISECONDS.toHours(uptimeMS);
        uptimeMS -= TimeUnit.HOURS.toMillis(uptimeHRS);
        long uptimeMIN = TimeUnit.MILLISECONDS.toMinutes(uptimeMS);
        uptimeMS -= TimeUnit.MINUTES.toMillis(uptimeMIN);
        long uptimeSEC = TimeUnit.MILLISECONDS.toSeconds(uptimeMS);

        long botMemTotal = Runtime.getRuntime().totalMemory() / 1048576;
        long botMemFree = Runtime.getRuntime().freeMemory() / 1048576;
        long botMemUsed = botMemTotal - botMemFree;

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(0x2F3136))
                .setDescription("**" + botName + "** - currently running version ``" + new Version().getVersion() + "``")
                .setThumbnail(botImage)
                .setAuthor(botName + " | Info", null, botImage)
                .addField("Name", "```" + botTag + "```", true)
                .addField("OS", "```" + botOS + "```", true)
                .addField("Library", "```JDA (Java)```", true)
                .addField("Ping", "```" + ping + "ms```", true)
                .addField("Uptime", "```" + uptimeDAYS + "d " + uptimeHRS + "h " + uptimeMIN + "min " + uptimeSEC + "s```", true)
                .addField("Memory (current Runtime)", "```" + botMemUsed + "MB / " + botMemTotal + "MB```", false)
                .setTimestamp(new Date().toInstant());

        event.replyEmbeds(eb.build()).queue();
    }

    public BotInfo() {
            this.name = "botinfo";
            this.aliases = new String[]{"bot"};
        this.category = new Category("USER COMMANDS");
            this.help = "Shows some information about Java";
    }

    @Override
    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}