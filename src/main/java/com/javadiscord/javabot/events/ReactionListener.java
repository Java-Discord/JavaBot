package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.bot;
import static com.javadiscord.javabot.events.Startup.mongoClient;

public class ReactionListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot() || event.getUser().getId().equals(bot.getId())) return;

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        BasicDBObject criteria = new BasicDBObject()
        .append("guild_id", event.getGuild().getId())
        .append("channel_id", event.getChannel().getId())
        .append("message_id", event.getMessageId())
        .append("emote", event.getReactionEmote().getName());

        try {
            String JSON = collection.find(criteria).first().toJson();

            JsonObject Root = JsonParser.parseString(JSON).getAsJsonObject();
            String roleID = Root.get("role_id").getAsString();
            Role role = event.getGuild().getRoleById(roleID);
            event.getGuild().addRoleToMember(event.getMember().getId(), role).queue();

        } catch (Exception e) {}
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (event.getMember().getUser().isBot()) return;

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");


        BasicDBObject criteria = new BasicDBObject()
                .append("guild_id", event.getGuild().getId())
                .append("channel_id", event.getChannel().getId())
                .append("message_id", event.getMessageId())
                .append("emote", event.getReactionEmote().getName());

        try {
            String JSON = collection.find(criteria).first().toJson();

            JsonObject Root = JsonParser.parseString(JSON).getAsJsonObject();
            String roleID = Root.get("role_id").getAsString();
            Role role = event.getGuild().getRoleById(roleID);
            event.getGuild().removeRoleFromMember(event.getMember().getId(), role).queue();

        } catch (Exception e) {}
    }

}
