package net.javadiscord.javabot.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for reactions in #looking-for-programmer.
 * Automatically deletes messages below a certain score.
 */
@Slf4j
public class JobChannelVoteListener extends ListenerAdapter {

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
		onReactionEvent(event);
	}

	@Override
	public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
		onReactionEvent(event);
	}

	private boolean isInvalidEvent(GenericMessageEvent genericEvent) {
		if (genericEvent instanceof MessageReactionAddEvent raEvent && raEvent.getUser() != null && (raEvent.getUser().isBot() || raEvent.getUser().isSystem())) {
			return true;
		}
		if (genericEvent.getChannelType() == ChannelType.PRIVATE) return true;
		return !genericEvent.getChannel().equals(Bot.config.get(genericEvent.getGuild()).getModeration().getJobChannel());
	}

	private void onReactionEvent(GenericMessageReactionEvent event) {
		if (event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem()) return;
		if (isInvalidEvent(event)) return;
		var config = Bot.config.get(event.getGuild());
		if (!event.getReactionEmote().isEmoji() || !event.getReactionEmote().getAsReactionCode().equals(config.getEmote().getJobChannelVoteEmoji())) return;
		event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
			int downvotes = message
					.getReactions()
					.stream()
					.filter(reaction -> reaction.getReactionEmote().getName().equals(config.getEmote().getJobChannelVoteEmoji()))
					.findFirst()
					.map(MessageReaction::getCount)
					.orElse(0);
			if (downvotes >= config.getModeration().getJobChannelMessageDeleteThreshold()) {
				message.delete().queue();
				message.getAuthor().openPrivateChannel()
						.queue(
								s -> s.sendMessage(String.format("Your message in %s has been removed due to community feedback.", config.getModeration().getJobChannel().getAsMention())).queue(),
								e -> {}
						);
			}
		});
	}
}
