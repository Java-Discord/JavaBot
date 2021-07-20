package com.javadiscord.javabot.commands.other.qotw;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import org.bson.Document;

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Correct {
    public static void correct(ButtonClickEvent event, Member member) {

        String check;
        TextChannel tc;
        tc = event.getGuild().getTextChannelById(Database.getConfigString(event.getGuild().getName(), event.getGuild().getId(), "channels.log_cid"));
        check = event.getGuild().getEmotesByName("check", false).get(0).getAsMention();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        int qotwPoints = Database.getMemberInt(collection, member, "qotwpoints");
        Database.queryMemberInt(member.getId(), "qotwpoints", qotwPoints + 1);

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("Question of the Week", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.GREEN)
                .setDescription("Your answer was correct! " + check + "\nYou've been granted **1 QOTW-Point!** (Total: " + (qotwPoints + 1) + ")")
                .setTimestamp(new Date().toInstant());

        try {
            member.getUser().openPrivateChannel().complete().sendMessage(eb.build()).queue();

            EmbedBuilder emb = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag() + " | QOTW-Point added", null, member.getUser().getEffectiveAvatarUrl())
                    .setColor(Constants.GREEN)
                    .addField("Total QOTW-Points", "```" + (qotwPoints + 1) + "```", true)
                    .addField("Rank", "```#" + Leaderboard.rank(member.getId()) + "```", true)
                    .setFooter("ID: " + member.getId())
                    .setTimestamp(new Date().toInstant());
            tc.sendMessage(emb.build()).queue();

        } catch (Exception e) {
            tc.sendMessage(Embeds.emptyError("```Couldn't send message <:abort:759740784882089995> (" + member.getUser().getAsTag() + ")```", event.getUser())).queue();
        }
    }
}
