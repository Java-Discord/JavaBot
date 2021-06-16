package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class GuildJoin extends ListenerAdapter {

    public static void addGuildToDB (String guildID, String guildName) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {

            String doc = collection.find(eq("guild_id", guildID)).first().toJson();
            JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();

        } catch (NullPointerException e) {

            collection.insertOne(Database.guildDoc(guildName, guildID));
            System.out.println("* Added Database entry for Guild \"" + guildName + "\" (" + guildID + ")");
        }
    }


    @Override
    public void onGuildJoin(GuildJoinEvent event) {

        addGuildToDB(event.getGuild().getId(), event.getGuild().getName());
    }
}
