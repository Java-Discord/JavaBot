package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.bson.Document;

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Warns {

    public static void execute(SlashCommandEvent event, Member member) {

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> collection = database.getCollection("users");
            int warnCount = Database.getMemberInt(collection, member, "warns");

            var e = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag() + " | Warns", null, member.getUser().getEffectiveAvatarUrl())
                    .setDescription(member.getAsMention() + " has been warned **" + warnCount + " times** so far.")
                    .setColor(Constants.YELLOW)
                    .setFooter("ID: " + member.getId())
                    .setTimestamp(new Date().toInstant())
                    .build();

            event.replyEmbeds(e).queue();
        }
    }
