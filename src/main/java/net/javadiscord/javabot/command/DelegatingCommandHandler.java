package net.javadiscord.javabot.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract command handler which is useful for commands which consist of lots
 * of subcommands. A child class will supply a map of subcommand handlers, so
 * that this parent handler can do the logic of finding the right subcommand to
 * invoke depending on the event received.
 */
public class DelegatingCommandHandler implements SlashCommand {
	private final Map<String, SlashCommand> subcommandHandlers;
	private final Map<String, SlashCommand> subcommandGroupHandlers;

	/**
	 * Constructs the handler with an already-initialized map of subcommands.
	 *
	 * @param subcommandHandlers The map of subcommands to use.
	 */
	public DelegatingCommandHandler(Map<String, SlashCommand> subcommandHandlers) {
		this.subcommandHandlers = subcommandHandlers;
		this.subcommandGroupHandlers = new HashMap<>();
	}

	/**
	 * Constructs the handler with an empty map, which subcommands can be added
	 * to via {@link DelegatingCommandHandler#addSubcommand(String, SlashCommand)}.
	 */
	public DelegatingCommandHandler() {
		this.subcommandHandlers = new HashMap<>();
		this.subcommandGroupHandlers = new HashMap<>();
	}

	/**
	 * Gets an unmodifiable map of the subcommand handlers this delegating
	 * handler has registered.
	 *
	 * @return An unmodifiable map containing all registered subcommands.
	 */
	public Map<String, SlashCommand> getSubcommandHandlers() {
		return Collections.unmodifiableMap(this.subcommandHandlers);
	}

	/**
	 * Gets an unmodifiable map of the subcommand group handlers that this
	 * handler has registered.
	 *
	 * @return An unmodifiable map containing all registered group handlers.
	 */
	public Map<String, SlashCommand> getSubcommandGroupHandlers() {
		return Collections.unmodifiableMap(this.subcommandGroupHandlers);
	}

	/**
	 * Adds a subcommand to this handler.
	 *
	 * @param name    The name of the subcommand. <em>This is case-sensitive.</em>
	 * @param handler The handler that will be called to handle subcommands with
	 *                the given name.
	 * @throws UnsupportedOperationException If this handler was initialized
	 *                                       with an unmodifiable map of subcommand handlers.
	 */
	protected void addSubcommand(String name, SlashCommand handler) {
		this.subcommandHandlers.put(name, handler);
	}

	/**
	 * Adds a subcommand group handler to this handler.
	 *
	 * @param name    The name of the subcommand group. <em>This is case-sensitive.</em>
	 * @param handler The handler that will be called to handle commands within
	 *                the given subcommand's name.
	 * @throws UnsupportedOperationException If this handler was initialized
	 *                                       with an unmodifiable map of subcommand group handlers.
	 */
	protected void addSubcommandGroup(String name, SlashCommand handler) {
		this.subcommandGroupHandlers.put(name, handler);
	}

	/**
	 * Handles slash command events by checking if a subcommand name was given,
	 * and if so, delegating the handling of the event to that subcommand.
	 *
	 * @param event The event.
	 * @return The reply action that is sent to the user.
	 */
	@Override
	public InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		// First we check if the event has specified a subcommand group, and if we have a group handler for it.
		if (event.getSubcommandGroup() != null) {
			SlashCommand groupHandler = this.getSubcommandGroupHandlers().get(event.getSubcommandGroup());
			if (groupHandler != null) {
				return groupHandler.handleSlashCommandInteraction(event);
			}
		}
		// If the event doesn't have a subcommand group, or no handler was found for the group, we just move on to the subcommand.
		if (event.getSubcommandName() == null) {
			return this.handleNonSubcommand(event);
		} else {
			SlashCommand handler = this.getSubcommandHandlers().get(event.getSubcommandName());
			if (handler != null) {
				return handler.handleSlashCommandInteraction(event);
			} else {
				return Responses.warning(event, "Unknown Subcommand", "The subcommand you entered could not be found.");
			}
		}
	}

	/**
	 * Handles the case where the main command is called without any subcommand.
	 *
	 * @param event The event.
	 * @return The reply action that is sent to the user.
	 */
	protected ReplyCallbackAction handleNonSubcommand(SlashCommandInteractionEvent event) {
		return Responses.warning(event, "Missing Subcommand", "Please specify a subcommand.");
	}
}
