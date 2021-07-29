package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class GuildJoin extends ListenerAdapter {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GuildJoin.class);

    public static void addGuildToDB (String guildID, String guildName) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        if (collection.find(eq("guild_id", guildID)).first() == null) {

            collection.insertOne(new Database().guildDoc(guildName, guildID));
            logger.warn("Added Database entry for Guild \"" + guildName + "\" (" + guildID + ")");
        }
    }


    @Override
    public void onGuildJoin(GuildJoinEvent event) {

        for (var guild : event.getJDA().getGuilds()) {
            Bot.slashCommands.registerSlashCommands(guild);
        }

        addGuildToDB(event.getGuild().getId(), event.getGuild().getName());
    }
}
