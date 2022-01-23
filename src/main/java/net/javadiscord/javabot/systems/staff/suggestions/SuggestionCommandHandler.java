package net.javadiscord.javabot.systems.staff.suggestions;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.AcceptSubcommand;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.ClearSubcommand;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.DeclineSubcommand;

/**
 * Handler class for all suggestion related commands.
 */
public class SuggestionCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler}.
	 */
	public SuggestionCommandHandler() {
		addSubcommand("accept", new AcceptSubcommand());
		addSubcommand("decline", new DeclineSubcommand());
		addSubcommand("clear", new ClearSubcommand());
	}
}
