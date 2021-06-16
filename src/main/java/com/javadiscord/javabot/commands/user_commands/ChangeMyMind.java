package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.Constants;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ChangeMyMind {

    public static void execute(SlashCommandEvent event, String content) {

        event.deferReply(false).queue();
        InteractionHook hook = event.getHook();

        String encodedSearchTerm = null;

            try {
                encodedSearchTerm = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) { e.printStackTrace(); }


        Unirest.get("https://nekobot.xyz/api/imagegen?type=changemymind&text=" + encodedSearchTerm).asJsonAsync(new Callback<JsonNode>(){

            @Override
            public void completed(HttpResponse<JsonNode> hr) {

                var e = new EmbedBuilder()
                        .setColor(Constants.GRAY)
                        .setImage(hr.getBody().getObject().getString("message"))
                        .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(new Date().toInstant())
                        .build();

                hook.sendMessageEmbeds(e).queue();
            }

            @Override
            public void failed(UnirestException ue) {
                // Shouldn't happen
            }

            @Override
            public void cancelled() {
                // Shouldn't happen
            }
        });
    }
}