package net.javadiscord.javabot.systems.staff.custom_commands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.staff.custom_commands.subcommands.CreateSubCommand;
import net.javadiscord.javabot.systems.staff.custom_commands.subcommands.DeleteSubCommand;
import net.javadiscord.javabot.systems.staff.custom_commands.subcommands.EditSubCommand;

/**
 * Handler class for the "/customcommand"-slash commands
 */
public class CustomCommandHandler extends DelegatingCommandHandler {

    /**
     * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand(String, SlashCommandHandler)}
     */
    public CustomCommandHandler() {
        addSubcommand("create", new CreateSubCommand());
        addSubcommand("delete", new DeleteSubCommand());
        addSubcommand("edit", new EditSubCommand());
    }

    /**
     * Cleans the given String by removing all whitespaces and slashes, so it can be used for custom commands.
     * @param s The string that should be cleaned.
     * @return The cleaned string.
     */
    public static String cleanString(String s) {
        return s.trim()
                .replaceAll("\\s+", "")
                .replace("/", "");
    }
}