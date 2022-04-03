package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.command.ResponseException;

/**
 * Interface that handles Discord's User Context Commands.
 */
public interface UserContextCommand {
	InteractionCallbackAction<InteractionHook> handleUserContextCommandInteraction(UserContextInteractionEvent event) throws ResponseException;
}
