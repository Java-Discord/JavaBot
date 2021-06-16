package com.javadiscord.javabot.commands.user_commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Lmgtfy {

    public static void execute(SlashCommandEvent event, String text) {

        String encodedSearchTerm = null;

        try {
             encodedSearchTerm = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) { e.printStackTrace(); }

            event.reply("<https://lmgtfy.com/?q=" + encodedSearchTerm + ">").queue();
    }
}