package com.javadiscord.javabot.commands.moderation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.bson.Document;

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Warns {

    public static void execute(SlashCommandEvent event, Member member) {

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> warns = database.getCollection("warns");

            StringBuilder sb = new StringBuilder();
            MongoCursor<Document> it = warns.find(eq("user_id", member.getId())).iterator();

            while (it.hasNext()) {

            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String uuID = root.get("uuid").getAsString();
            String reason = root.get("reason").getAsString();
            String date = root.get("date").getAsString();

            sb.append("[Date] " + date +
                    "\n[Reason] " + reason +
                    "\n[UUID] " + uuID + "\n\n");
            }

            var e = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag() + " | Warns", null, member.getUser().getEffectiveAvatarUrl())
                    .setDescription("```" + member.getUser().getAsTag() + " has been warned " + warns.count(eq("user_id", member.getId())) + " times so far."
                    + "\n\n" + sb + "```")
                    .setColor(Constants.YELLOW)
                    .setFooter("ID: " + member.getId())
                    .setTimestamp(new Date().toInstant())
                    .build();

            event.replyEmbeds(e).queue();
        }
    }
