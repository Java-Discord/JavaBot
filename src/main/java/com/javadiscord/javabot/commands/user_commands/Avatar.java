package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;

public class Avatar implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        OptionMapping option = event.getOption("user");
        User user = option == null ? event.getUser() : option.getAsUser();
        EmbedBuilder eb = new EmbedBuilder()
            .setAuthor(user.getAsTag() + " | Avatar")
            .setColor(new Color(0x2F3136))
            .setImage(user.getEffectiveAvatarUrl() + "?size=4096");
        event.replyEmbeds(eb.build()).queue();
    }
}
