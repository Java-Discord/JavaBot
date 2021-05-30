package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Database;
import com.mongodb.MongoSocketReadException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Date;

public class SuggestionListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        try {
            if (event.getMember().getUser().isBot()) return;
        } catch (NullPointerException e) { }

        try {
            if (event.getChannel().getId().equals(Database.getConfigString(event, "suggestion_cid"))) {

                String[] args = event.getMessage().getContentDisplay().split(" ");
                if (!args[0].startsWith("!")) {

                    String Content = event.getMessage().getContentDisplay();
                    event.getMessage().delete().queue();


                    Emote upvote = event.getGuild().getEmotesByName("up_vote", false).get(0);
                    Emote downvote = event.getGuild().getEmotesByName("down_vote", false).get(0);

                    EmbedBuilder eb = new EmbedBuilder()
                            .setAuthor(event.getAuthor().getAsTag() + " · Suggestion", null, event.getAuthor().getEffectiveAvatarUrl())
                            .setDescription(Content)
                            .setTimestamp(new Date().toInstant())
                            .setColor(new Color(0x2F3136));

                    event.getChannel().sendMessage(eb.build()).queue(message -> {
                        message.addReaction(upvote).queue();
                        message.addReaction(downvote).queue();
                    });
                }
            }


        } catch (MongoSocketReadException e) {
        }
    }
}
