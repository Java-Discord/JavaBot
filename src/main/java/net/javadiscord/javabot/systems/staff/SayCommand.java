package net.javadiscord.javabot.systems.staff;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.SlashCommandHandler;

public class SayCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String text = event.getOption("text").getAsString();

        event.getChannel().sendMessage(text).queue();
        return event.reply("Done!").setEphemeral(true);
    }
}