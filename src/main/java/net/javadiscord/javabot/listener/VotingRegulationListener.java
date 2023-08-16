package net.javadiscord.javabot.listener;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;

/**
 * Makes sure users don't vote on/star their own messages.
 */
@RequiredArgsConstructor
public class VotingRegulationListener extends ListenerAdapter{

	private final BotConfig botConfig;

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getUser().isBot() || event.getUser().isSystem()) {
			return;
		}
		if(isCriticalEmoji(event)) {
			event.retrieveMessage().queue(msg->{
				if(doesAuthorMatch(event.getUserIdLong(), msg)) {
					msg.removeReaction(event.getEmoji(), event.getUser()).queue();
				}
			});
		}
	}

	private boolean doesAuthorMatch(long userId, Message msg) {
		long suggestionChannelId = botConfig.get(msg.getGuild()).getModerationConfig().getSuggestionChannelId();
		return msg.getAuthor().getIdLong() == userId||
				msg.getChannel().getIdLong() == suggestionChannelId &&
				!msg.getEmbeds().isEmpty() &&
				msg.getEmbeds().get(0).getFooter() != null &&
				String.valueOf(userId).equals(msg.getEmbeds().get(0).getFooter().getText());
	}

	private boolean isCriticalEmoji(MessageReactionAddEvent event) {
		return event.getEmoji().equals(getUpvoteEmoji(event)) ||
				event.getEmoji().getType() == Emoji.Type.UNICODE &&
				botConfig.get(event.getGuild()).getStarboardConfig().getEmojis().contains(event.getEmoji().asUnicode());
	}

	private Emoji getUpvoteEmoji(MessageReactionAddEvent event) {
		return botConfig.getSystems().getEmojiConfig().getUpvoteEmote(event.getJDA());
	}
}
