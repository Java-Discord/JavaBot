package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Google implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String query;
        String url;
        try {
            query = URLEncoder.encode(Objects.requireNonNull(event.getOption("query")).getAsString(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return event.reply(e.getMessage());
        }
        query = query.trim().replace(" ", "+");
        url = "https://www.google.com/search?q=".concat(query);
        return event.reply(url);
    }
}
