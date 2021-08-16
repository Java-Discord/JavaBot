package com.javadiscord.javabot.commands.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Version implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        return event.reply(getVersion(event.getJDA()));
    }

    public String getVersion (JDA jda) {

        try {
            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("config");

            String doc = collection.find(eq("name", jda.getSelfUser().getAsTag())).first().toJson();
            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();

            return root.get("version").getAsString();

        } catch (Exception e) {
            return "v00-00.00";
        }
    }
}
