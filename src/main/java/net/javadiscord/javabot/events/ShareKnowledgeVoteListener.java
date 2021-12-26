package net.javadiscord.javabot.events;

import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for messages and reactions in #share-knowledge.
 * Automatically deletes messages below a certain score.
 */
public class ShareKnowledgeVoteListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (isInvalidEvent(event)) return;

        var config = Bot.config.get(event.getGuild());

        // add upvote and downvote option
        event.getMessage().addReaction(config.getEmote().getUpvoteEmote()).queue();
        event.getMessage().addReaction(config.getEmote().getDownvoteEmote()).queue();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        onReactionEvent(event);
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        onReactionEvent(event);
    }

    private boolean isInvalidEvent(GenericMessageEvent genericEvent) {
        if (genericEvent instanceof MessageReceivedEvent event &&
                (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.getMessage().getType() == MessageType.THREAD_CREATED)) {
            return true;
        }
        if (genericEvent instanceof MessageReactionAddEvent raEvent && raEvent.getUser() != null && (raEvent.getUser().isBot() || raEvent.getUser().isSystem())) {
            return true;
        }
        return !genericEvent.getChannel().equals(Bot.config.get(genericEvent.getGuild()).getModeration().getShareKnowledgeChannel());
    }

    private void onReactionEvent (GenericMessageReactionEvent event) {
        if (event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem()) return;
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
