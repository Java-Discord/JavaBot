package com.javadiscord.javabot.commands.staff_commands.custom_commands;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.staff_commands.custom_commands.subcommands.CustomCommandCreate;
import com.javadiscord.javabot.commands.staff_commands.custom_commands.subcommands.CustomCommandDelete;
import com.javadiscord.javabot.commands.staff_commands.custom_commands.subcommands.CustomCommandEdit;
import com.mongodb.BasicDBObject;

import static com.javadiscord.javabot.service.Startup.mongoClient;

/**
 * Handler class for the "/customcommand"-slash commands
 */
public class CustomCommands extends DelegatingCommandHandler {

    /**
     * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand(String, SlashCommandHandler)}
     */
    public CustomCommands() {
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
        return mongoClient.getDatabase("other")
                .getCollection("customcommands")
                .find(
                new BasicDBObject()
                        .append("guildId", guildId)
                        .append("commandName", commandName))
                .first() != null;
    }
}