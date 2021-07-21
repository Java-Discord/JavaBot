package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Date;

public class SuggestionListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
            if (event.getMember().getUser().isBot() || event.getMember() == null) return;
            if (event.getChannel().getId().equals(new Database().getConfigString(event.getGuild(), "channels.suggestion_cid"))) {

                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Constants.GRAY)
                            .setImage(null)
                            .setAuthor(event.getAuthor().getAsTag() + " · Suggestion", null, event.getAuthor().getEffectiveAvatarUrl())
                            .setTimestamp(new Date().toInstant())
                            .setDescription(event.getMessage().getContentRaw());

                        if (!event.getMessage().getAttachments().isEmpty()) {
                            Message.Attachment attachment = event.getMessage().getAttachments().get(0);

                            try {
                                event.getChannel().sendFile(attachment.retrieveInputStream().get(), "attachment." + attachment.getFileExtension()).setEmbeds(eb.build()).queue(message -> {
                                    message.addReaction(Constants.REACTION_UPVOTE).queue();
                                    message.addReaction(Constants.REACTION_DOWNVOTE).queue();
                                });

                            } catch (Exception e) { event.getChannel().sendMessageEmbeds(Embeds.emptyError(event.getAuthor().getAsMention() + ": ```" + e.getMessage() + "```", event.getAuthor())).queue(); }

                        } else {

                            event.getChannel().sendMessageEmbeds(eb.build()).queue(message -> {
                                message.addReaction(Constants.REACTION_UPVOTE).queue();
                                message.addReaction(Constants.REACTION_DOWNVOTE).queue();
                            });
                        }

                    event.getMessage().delete().queue();
            }
        }
    }
