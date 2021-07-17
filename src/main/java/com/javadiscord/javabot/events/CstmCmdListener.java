package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.other.Constants;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class CstmCmdListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        try {
            if (event.getMember().getUser().isBot()) return;
        } catch (NullPointerException e) { }

        String[] args = event.getMessage().getContentDisplay().split(" ");
            if (!args[0].startsWith("!")) return;

                MongoDatabase database = mongoClient.getDatabase("other");
                MongoCollection<Document> collection = database.getCollection("customcommands");

                String commandName = args[0].toLowerCase().substring(1, args[0].length());

                BasicDBObject criteria = new BasicDBObject()
                        .append("guild_id", event.getGuild().getId())
                        .append("commandname", commandName);

                try {
                    String json = collection.find(criteria).first().toJson();

                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    String value = root.get("value").getAsString();
                    boolean deleteMessage = root.get("delete_message").getAsBoolean();

                    if (deleteMessage) event.getMessage().delete().complete();

                    String text = value
                            .replace("{!membercount}", String.valueOf(event.getGuild().getMemberCount()))
                            .replace("{!servername}", event.getGuild().getName())
                            .replace("{!serverid}", event.getGuild().getId());

                    var e = new EmbedBuilder()
                            .setColor(Constants.GRAY)
                            .setDescription(text)
                            .build();

                    event.getChannel().sendMessageEmbeds(e).queue();

                } catch (NullPointerException ignored) { }

        }
    }

