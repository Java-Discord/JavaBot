package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;

public class Help implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var e = new EmbedBuilder()
            .setDescription("Visit **[this page](" + Constants.HELP_LINK + ")** for a full list of Commands \n \n *Please note that this site is deprecated as we moved to Slash-Commands in [#11](https://github.com/Java-Discord/JavaBot/pull/11)*")
            .setColor(Color.decode(
                    Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor()))
            .build();

        return event.replyEmbeds(e);
    }
}