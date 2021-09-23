package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.other.Version;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Date;

public class BotInfo implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        long ping = event.getJDA().getGatewayPing();
        var bot = event.getJDA().getSelfUser();

        var e = new EmbedBuilder()
            .setColor(Constants.GRAY)
            .setThumbnail(bot.getEffectiveAvatarUrl())
            .setAuthor(bot.getName() + " | Info", null, bot.getEffectiveAvatarUrl())
            .setDescription("**" + bot.getName() + "** - currently running Version `" + new Version().getVersion(event.getJDA()) + "`")
            .addField("OS", "```" + System.getProperty("os.name") + "```", true)
            .addField("Library", "```JDA```", true)
            .addField("JDK", "```" + System.getProperty("java.version") + "```", true)
            .addField("Ping", "```" + ping + "ms```", true)
            .addField("Uptime", "```" + new Uptime().getUptime() + "```", true)
            .setTimestamp(new Date().toInstant())
            .build();

        return event.replyEmbeds(e).addActionRow(
                Button.link(Constants.GITHUB_LINK, "View on GitHub")
        );
    }
}
