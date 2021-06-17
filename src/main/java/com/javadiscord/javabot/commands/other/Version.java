package com.javadiscord.javabot.commands.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Version implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        event.reply(getVersion()).queue();
    }

    public String getVersion () {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");
        String doc = collection.find(eq("name", "Java#9523")).first().toJson();
        JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
        return Root.get("version").getAsString();
    }
}
