package com.javadiscord.javabot.commands.user_commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;

public class Avatar {

    public static void execute(SlashCommandEvent event, User user) {

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(user.getAsTag() + " | Avatar")
                .setColor(new Color(0x2F3136))
                .setImage(user.getEffectiveAvatarUrl() + "?size=4096");

        event.replyEmbeds(eb.build()).queue();

    }
}
