package net.discordjug.javabot.systems.help.checks;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

/**
 * Simple data class that represents a single help channel.
 *
 * @param initialMessage        The help channel's initial message.
 * @param timeSinceFirstMessage The time since the initial message.
 * @param nonOwnerParticipants  All members that participated, excluding the owner.
 * @param botMessages           All messages that the bot sent.
 */
public record ChannelSemanticData(
		@Nullable Message initialMessage,
		Duration timeSinceFirstMessage,
		List<User> nonOwnerParticipants,
		List<Message> botMessages
) {
	public boolean containsBotMessageContent(String content) {
		return botMessages.stream()
				.anyMatch(m -> m.getContentRaw().contains(content));
	}
}
