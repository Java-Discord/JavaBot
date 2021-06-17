package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class Help implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        var e = new EmbedBuilder()
            .setDescription("Visit **[this page](" + Constants.HELP_LINK + ")** for a full list of Commands")
            .setColor(Constants.GRAY)
            .build();

        event.replyEmbeds(e).queue();
    }
}