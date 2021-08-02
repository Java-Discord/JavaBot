package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class Avatar implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        OptionMapping option = event.getOption("user");
        User user = option == null ? event.getUser() : option.getAsUser();

        EmbedBuilder eb = new EmbedBuilder()
            .setColor(Constants.GRAY)
            .setAuthor(user.getAsTag() + " | Avatar")
            .setImage(user.getEffectiveAvatarUrl() + "?size=4096");
        return event.replyEmbeds(eb.build());
    }
}
