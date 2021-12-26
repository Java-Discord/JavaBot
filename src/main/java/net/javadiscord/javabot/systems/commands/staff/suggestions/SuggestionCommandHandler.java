package net.javadiscord.javabot.systems.commands.staff.suggestions;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.commands.staff.suggestions.subcommands.AcceptSubcommand;
import net.javadiscord.javabot.systems.commands.staff.suggestions.subcommands.ClearSubcommand;
import net.javadiscord.javabot.systems.commands.staff.suggestions.subcommands.DeclineSubcommand;

/**
 * Command to modify the state of suggestions.
 */
public class SuggestionCommandHandler extends DelegatingCommandHandler {

    public SuggestionCommandHandler() {
        addSubcommand("accept", new AcceptSubcommand());
        addSubcommand("decline", new DeclineSubcommand());
        addSubcommand("clear", new ClearSubcommand());
    }
}
