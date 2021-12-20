package net.javadiscord.javabot.systems.commands.staff.custom_commands;

import com.mongodb.BasicDBObject;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.events.StartupListener;
import net.javadiscord.javabot.systems.commands.staff.custom_commands.subcommands.CustomCommandCreate;
import net.javadiscord.javabot.systems.commands.staff.custom_commands.subcommands.CustomCommandDelete;
import net.javadiscord.javabot.systems.commands.staff.custom_commands.subcommands.CustomCommandEdit;

/**
 * Handler class for the "/customcommand"-slash commands
 */
public class CustomCommandHandler extends DelegatingCommandHandler {

    /**
     * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand(String, SlashCommandHandler)}
     */
    public CustomCommandHandler() {
        addSubcommand("create", new CustomCommandCreate());
        addSubcommand("delete", new CustomCommandDelete());
        addSubcommand("edit", new CustomCommandEdit());
    }

    /**
     * Checks if a custom command with the specified name exists.
     * @param guildId The guild's id
     * @param commandName The name of the custom slash command
     */
    public static boolean commandExists(String guildId, String commandName) {
        return StartupListener.mongoClient.getDatabase("other")
                .getCollection("customcommands")
                .find(
                new BasicDBObject()
                        .append("guildId", guildId)
                        .append("commandName", commandName))
                .first() != null;
    }
}