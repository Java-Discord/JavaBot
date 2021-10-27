package com.javadiscord.javabot.commands.custom_commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.time.Instant;

import static com.javadiscord.javabot.service.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class CustomCommandList implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        StringBuilder sb = new StringBuilder();
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("customcommands");
        MongoCursor<Document> it = collection.find(eq("guild_id", event.getGuild().getId())).iterator();

        while (it.hasNext()) {

            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String commandName = root.get("commandname").getAsString();

            sb.append("/").append(commandName).append("\n");
        }

        String description;

        if (sb.length() > 0) description = "```css\n" + sb + "```";
        else description = "```No Custom Commands created yet.```";

        var e = new EmbedBuilder()
                .setTitle("Custom Slash Command List")
                .setDescription(description)
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setTimestamp(Instant.now())
                .build();

        return event.replyEmbeds(e);
    }
}
