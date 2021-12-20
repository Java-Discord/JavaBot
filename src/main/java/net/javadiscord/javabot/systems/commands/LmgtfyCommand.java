package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LmgtfyCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        String encodedSearchTerm = null;

        try {
            encodedSearchTerm = URLEncoder.encode(Objects.requireNonNull(event.getOption("text")).getAsString(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) { e.printStackTrace(); }

        return event.reply("<https://lmgtfy.com/?q=" + encodedSearchTerm + ">");
    }
}