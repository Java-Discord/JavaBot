package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

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
