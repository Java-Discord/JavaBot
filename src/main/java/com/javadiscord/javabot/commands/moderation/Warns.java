package com.javadiscord.javabot.commands.moderation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.awt.*;
import java.time.Instant;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Warns implements SlashCommandHandler {

    public long warnCount (Member member) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        BasicDBObject criteria = new BasicDBObject()
                .append("guild_id", member.getGuild().getId())
                .append("user_id", member.getId());

        return warns.countDocuments(criteria);
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping warnsOption = event.getOption("user");
        Member member = warnsOption == null ? event.getMember() : warnsOption.getAsMember();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        StringBuilder sb = new StringBuilder();

        for (Document document : warns.find(eq("user_id", member.getId()))) {
            JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String reason = root.get("reason").getAsString();
            String date = root.get("date").getAsString();
            sb.append("[Date] ").append(date).append("\n[Reason] ").append(reason).append("\n\n");
        }

        var e = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Warns", null, member.getUser().getEffectiveAvatarUrl())
            .setDescription("```" + member.getUser().getAsTag() + " has been warned " + warnCount(member) + " times so far."
                + "\n\n" + sb + "```")
            .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getWarningColor())
            .setFooter("ID: " + member.getId())
            .setTimestamp(Instant.now())
            .build();

        return event.replyEmbeds(e);
    }
}
