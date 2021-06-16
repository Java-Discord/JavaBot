package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ChangeMyMind extends Command {

    public static void exCommand (CommandEvent event) {

        List<Emote> Emote = event.getGuild().getEmotesByName("loading", false);
        event.getMessage().addReaction(Emote.get(0)).complete();

        String[] args = event.getArgs().split("\\s+");
        String encodedSearchTerm = null;

        try {
            String[] argRange = Arrays.copyOfRange(args, 0, args.length);
            StringBuilder builder = new StringBuilder();
            for (String value : argRange) {
                builder.append(value + " ");
            }

            String searchTerm = builder.substring(0, builder.toString().length() - 1);
            encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());

        } catch (Exception e) {
            Embeds.syntaxError("chmm Text", event);
        }

        Unirest.get("https://nekobot.xyz/api/imagegen?type=changemymind&text=" + encodedSearchTerm).asJsonAsync(new Callback<JsonNode>(){

            @Override
            public void completed(HttpResponse<JsonNode> hr) {

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(new Color(0x2F3136))
                        .setImage(hr.getBody().getObject().getString("message"))
                        .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                        .setTimestamp(new Date().toInstant());
                event.getMessage().reply(eb.build()).queue();
                event.getMessage().clearReactions().complete();
            }

            @Override
            public void failed(UnirestException ue) {
                event.reactError();
            }

            @Override
            public void cancelled() {
                event.reactError();
            }
        });
    }

    public static void exCommand (SlashCommandEvent event, String content) {

        event.deferReply(false).queue();
        InteractionHook hook = event.getHook();

        String encodedSearchTerm = null;

            try {
                encodedSearchTerm = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());

            } catch (UnsupportedEncodingException e) { e.printStackTrace(); }


        Unirest.get("https://nekobot.xyz/api/imagegen?type=changemymind&text=" + encodedSearchTerm).asJsonAsync(new Callback<JsonNode>(){

            @Override
            public void completed(HttpResponse<JsonNode> hr) {

                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Constants.GRAY)
                        .setImage(hr.getBody().getObject().getString("message"))
                        .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(new Date().toInstant());

                hook.sendMessageEmbeds(eb.build()).queue();
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

    public ChangeMyMind() {
        this.name = "chmm";
        this.aliases = new String[]{"changemymind", "changemm"};
        this.category = new Category("USER COMMANDS");
        this.arguments = "<Text>";
        this.help = "Generates the \"change my mind\" meme out of your given input";
    }

    @Override
    protected void execute(CommandEvent event) {

        exCommand(event);
    }
}