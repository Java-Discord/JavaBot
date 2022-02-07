package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;

/**
 * Interface that handles Discord's Message Context Commands.
 */
public interface IMessageContextCommand {
	ReplyCallbackAction handleMessageContextCommandInteraction(MessageContextInteractionEvent event) throws ResponseException;
}
