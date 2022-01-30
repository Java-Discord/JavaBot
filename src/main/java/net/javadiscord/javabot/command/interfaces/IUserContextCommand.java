package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;

/**
 * Interface that handles Discord's User Context Commands.
 */
public interface IUserContextCommand {
	ReplyCallbackAction handleUserContextCommandInteraction(UserContextInteractionEvent event) throws ResponseException;
}
