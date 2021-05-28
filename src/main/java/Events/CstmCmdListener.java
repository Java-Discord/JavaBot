package Events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.awt.*;

import static Events.Startup.mongoClient;

public class CstmCmdListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        try {
            if (event.getMember().getUser().isBot()) return;
        } catch (NullPointerException e) { }

        String[] args = event.getMessage().getContentDisplay().split(" ");
            if (args[0].startsWith("!")) {

                MongoDatabase database = mongoClient.getDatabase("other");
                MongoCollection<Document> collection = database.getCollection("customcommands");

                String commandName = args[0].toLowerCase().substring(1, args[0].length());

                BasicDBObject criteria = new BasicDBObject()
                        .append("guild_id", event.getGuild().getId())
                        .append("commandname", commandName);

                try {
                    String JSON = collection.find(criteria).first().toJson();

                    JsonObject Root = JsonParser.parseString(JSON).getAsJsonObject();
                    String Value = Root.get("value").getAsString();
                    boolean deleteMessage = Root.get("delete_message").getAsBoolean();

                    if (deleteMessage) event.getMessage().delete().complete();

                    String text = Value
                            .replace("{!membercount}", String.valueOf(event.getGuild().getMemberCount()))
                            .replace("{!servername}", event.getGuild().getName())
                            .replace("{!serverid}", event.getGuild().getId());

                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(new Color(0x2F3136))
                            .setDescription(text);
                    event.getChannel().sendMessage(eb.build()).queue();

                } catch (NullPointerException ignored) { }
            }
        }
    }

