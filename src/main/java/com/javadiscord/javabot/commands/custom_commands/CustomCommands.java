package com.javadiscord.javabot.commands.custom_commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.custom_commands.subcommands.CustomCommandCreate;
import com.javadiscord.javabot.commands.custom_commands.subcommands.CustomCommandDelete;
import com.javadiscord.javabot.commands.custom_commands.subcommands.CustomCommandEdit;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.time.Instant;

import static com.javadiscord.javabot.service.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class CustomCommands extends DelegatingCommandHandler {

    public CustomCommands() {
        addSubcommand("create", new CustomCommandCreate());
        addSubcommand("delete", new CustomCommandDelete());
        addSubcommand("edit", new CustomCommandEdit());
    }

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