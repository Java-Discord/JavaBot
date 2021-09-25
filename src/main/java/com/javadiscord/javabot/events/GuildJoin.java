package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class GuildJoin extends ListenerAdapter {

    public static void addGuildToDB (Guild guild) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        // TODO: fix this using the new file based config

        if (collection.find(eq("guild_id", guild.getId())).first() == null) {
            //new Database().insertGuildDoc(guild);
        }
    }


    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Bot.config.addGuild(event.getGuild());
        for (var guild : event.getJDA().getGuilds()) {
            Bot.slashCommands.registerSlashCommands(guild);
        }

        addGuildToDB(event.getGuild());
    }
}
