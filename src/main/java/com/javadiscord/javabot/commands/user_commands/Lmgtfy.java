package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Lmgtfy implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String encodedSearchTerm = null;

        try {
            encodedSearchTerm = URLEncoder.encode(Objects.requireNonNull(event.getOption("text")).getAsString(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) { e.printStackTrace(); }

        event.reply("<https://lmgtfy.com/?q=" + encodedSearchTerm + ">").queue();
    }
}