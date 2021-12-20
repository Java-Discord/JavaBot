package net.javadiscord.javabot.events;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.mongodb.Database;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class GuildJoinListener extends ListenerAdapter {

    public static void addGuildToDB (Guild guild) {

        MongoDatabase database = StartupListener.mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        if (collection.find(eq("guild_id", guild.getId())).first() == null) {
            new Database().insertGuildDoc(guild);
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
