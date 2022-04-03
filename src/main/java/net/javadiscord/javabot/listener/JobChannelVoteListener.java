package net.javadiscord.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
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
		if (event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem()) return;
		var config = Bot.config.get(event.getGuild());
		if (!event.getReactionEmote().isEmoji() || !event.getReactionEmote().getAsReactionCode().equals(config.getEmote().getJobChannelVoteEmoji())) return;
		if (!event.getTextChannel().equals(config.getModeration().getJobChannel())) return;
		event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
			int votes = message
					.getReactions()
					.stream()
					.filter(reaction -> reaction.getReactionEmote().getName().equals(config.getEmote().getJobChannelVoteEmoji()))
					.findFirst()
					.map(MessageReaction::getCount)
					.orElse(0);
			if (votes >= config.getModeration().getJobChannelMessageDeleteThreshold()) {
				message.delete().queue();
				message.getAuthor().openPrivateChannel()
						.queue(
								s -> s.sendMessageFormat("Your message in %s has been removed due to community feedback.", config.getModeration().getJobChannel().getAsMention()).queue(),
								e -> {}
						);
			}
		});
	}
}
