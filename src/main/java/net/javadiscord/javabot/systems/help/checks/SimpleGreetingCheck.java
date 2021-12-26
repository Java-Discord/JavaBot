package net.javadiscord.javabot.systems.help.checks;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.javadiscord.javabot.systems.help.ChannelSemanticCheck;
import net.javadiscord.javabot.systems.help.ChannelSemanticData;

import java.util.List;

/**
 * Checks if the user just sent only a single greeting and no other information,
 * and replies to their message asking them to elaborate on what their question
 * is.
 */
public class SimpleGreetingCheck implements ChannelSemanticCheck {
	private static final String[] GREETINGS = {"hi", "hello", "hey", "yo"};
	private static final String MESSAGE = "Hi there! It would be helpful if you could provide a detailed description of your problem.";

	@Override
	public RestAction<?> doCheck(TextChannel channel, User owner, List<Message> messages, ChannelSemanticData semanticData) {
		if (semanticData.initialMessage() != null && messages.size() == 1 && !semanticData.containsBotMessageContent(MESSAGE)) {
			var m = semanticData.initialMessage();
			String content = m.getContentStripped().toLowerCase();
			if (content.length() < 25) {
				for (var g : GREETINGS) {
					if (content.contains(g)) {
						return m.reply(MESSAGE);
					}
				}
			}
		}
		return new CompletedRestAction<>(channel.getJDA(), null);
	}
}
