package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for messages and reactions in #share-knowledge.
 * Automatically deletes messages below a certain score.
 */
public class ShareKnowledgeVoteListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (isInvalidEvent(event)) return;

        var config = Bot.config.get(event.getGuild());

        // add upvote and downvote option
        event.getMessage().addReaction(config.getEmote().getUpvoteEmote()).queue();
        event.getMessage().addReaction(config.getEmote().getDownvoteEmote()).queue();
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        onReactionEvent(event);
    }

    @Override
    public void onGuildMessageReactionRemove (@NotNull GuildMessageReactionRemoveEvent event) {
        onReactionEvent(event);
    }

    private boolean isInvalidEvent(GenericGuildMessageEvent genericEvent) {
        if (genericEvent instanceof GuildMessageReceivedEvent event)
            if (event.getAuthor().isBot() || event.getAuthor().isSystem()
                    || event.getMessage().getType() == MessageType.THREAD_CREATED) return true;

        return !genericEvent.getChannel().equals(
                Bot.config.get(genericEvent.getGuild()).getModeration().getShareKnowledgeChannel());
    }

    private void onReactionEvent (GenericGuildMessageReactionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) return;
        if (isInvalidEvent(event)) return;

        var config = Bot.config.get(event.getGuild());

        String reactionID = event.getReaction().getReactionEmote().getAsReactionCode();

        String upvoteID = config.getEmote().getUpvoteEmote().getAsMention();
        String downvoteID = config.getEmote().getDownvoteEmote().getAsMention();

        if (!(reactionID.equals(upvoteID) || reactionID.equals(downvoteID))) return;

        String messageID = event.getMessageId();
        event.getChannel().retrieveMessageById(messageID).queue(message->{
            int upvotes = message
                    .getReactions()
                    .stream()
                    .filter(reaction -> reaction.getReactionEmote().getAsReactionCode().equals(upvoteID))
                    .findFirst()
                    .map(MessageReaction::getCount)
                    .orElse(0);

            int downvotes = message
                    .getReactions()
                    .stream()
                    .filter(reaction -> reaction.getReactionEmote().getAsReactionCode().equals(downvoteID))
                    .findFirst()
                    .map(MessageReaction::getCount)
                    .orElse(0);

            int eval = downvotes - upvotes;

            if (eval >= config.getModeration().getShareKnowledgeMessageDeleteThreshold()) {
                message.delete().queue();
                message.getAuthor().openPrivateChannel()
                        .queue(channel -> channel.sendMessage("Your Message in " +
                                config.getModeration().getShareKnowledgeChannel().getAsMention() +
                                " has been removed due to community feedback").queue());
            }
        });
    }
}
