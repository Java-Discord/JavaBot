package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.MongoSocketReadException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Date;

public class SuggestionListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        try {
            if (event.getMember().getUser().isBot()) return;
        } catch (NullPointerException e) { }

            if (event.getChannel().getId().equals(Database.getConfigString(event, "suggestion_cid"))) {

                String[] args = event.getMessage().getContentDisplay().split(" ");
                if (!args[0].startsWith("!")) {

                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Constants.GRAY)
                            .setAuthor(event.getAuthor().getAsTag() + " · Suggestion", null, event.getAuthor().getEffectiveAvatarUrl())
                            .setTimestamp(new Date().toInstant())
                            .setDescription(event.getMessage().getContentRaw());

                    try {

                        Message.Attachment attachment = event.getMessage().getAttachments().get(0);

                        if (attachment.isImage()) {

                            eb.setImage(event.getMessage().getAttachments().get(0).getUrl());
                        } else if (attachment.isVideo()) {

                            eb.setImage(null);

                            if (event.getMessage().getContentRaw().isBlank()) eb.setDescription(attachment.getUrl());
                            else eb.setDescription(attachment.getUrl() + "\n\n" + eb.build().getDescription());
                        }

                        event.getMessage().delete().queue();


                        event.getChannel().sendMessage(eb.build()).queue(message -> {
                            message.addReaction(Constants.REACTION_UPVOTE).queue();
                            message.addReaction(Constants.REACTION_DOWNVOTE).queue();
                        });

                    } catch (IndexOutOfBoundsException e) {

                        event.getChannel().sendMessage(eb.build()).queue(message -> {
                            message.addReaction(Constants.REACTION_UPVOTE).queue();
                            message.addReaction(Constants.REACTION_DOWNVOTE).queue();
                        });

                    }
                }
            }
        }
    }
