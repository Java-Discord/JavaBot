package com.javadiscord.javabot.commands.staff_commands.suggestions;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.staff_commands.suggestions.subcommands.AcceptSubcommand;
import com.javadiscord.javabot.commands.staff_commands.suggestions.subcommands.ClearSubcommand;
import com.javadiscord.javabot.commands.staff_commands.suggestions.subcommands.DeclineSubcommand;

/**
 * Command to modify the state of suggestions.
 */
public class Suggestion extends DelegatingCommandHandler {

    public Suggestion() {
        addSubcommand("accept", new AcceptSubcommand());
        addSubcommand("decline", new DeclineSubcommand());
        addSubcommand("clear", new ClearSubcommand());
    }
}
