package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.command.ResponseException;

/**
 * Interface that handles Discord's Message Context Commands.
 */
public interface IMessageContextCommand {
	InteractionCallbackAction<InteractionHook> handleMessageContextCommandInteraction(MessageContextInteractionEvent event) throws ResponseException;
}
