package com.javadiscord.javabot.commands.moderation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Warns implements SlashCommandHandler {

    public long warnCount (Member member) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        return warns.countDocuments(eq("user_id", member.getId()));
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping warnsOption = event.getOption("user");
        Member member = warnsOption == null ? event.getMember() : warnsOption.getAsMember();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        StringBuilder sb = new StringBuilder();
        MongoCursor<Document> it = warns.find(eq("user_id", member.getId())).iterator();

        while (it.hasNext()) {
            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String reason = root.get("reason").getAsString();
            String date = root.get("date").getAsString();
            sb.append("[Date] " + date +
                "\n[Reason] " + reason + "\n\n");
        }

        var e = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Warns", null, member.getUser().getEffectiveAvatarUrl())
            .setDescription("```" + member.getUser().getAsTag() + " has been warned " + warnCount(member) + " times so far."
                + "\n\n" + sb + "```")
            .setColor(Constants.YELLOW)
            .setFooter("ID: " + member.getId())
            .setTimestamp(new Date().toInstant())
            .build();

        return event.replyEmbeds(e);
    }
}
