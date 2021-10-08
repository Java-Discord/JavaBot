package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class Say implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String text = event.getOption("text").getAsString();

        event.getChannel().sendMessage(text).queue();
        return event.reply("Done!").setEphemeral(true);
    }
}