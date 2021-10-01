package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Misc;
import com.javadiscord.javabot.other.TimeUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Warn implements SlashCommandHandler {

    public void addToDatabase(String memID, String guildID, String reason) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        Document doc = new Document("guild_id", guildID)
                .append("user_id", memID)
                .append("date", LocalDateTime.now().format(TimeUtils.STANDARD_FORMATTER))
                .append("reason", reason);

        warns.insertOne(doc);
    }

    public void deleteAllDocs(String memID) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");
        MongoCursor<Document> it = warns.find(eq("user_id", memID)).iterator();

        while (it.hasNext()) { warns.deleteOne(it.next()); }
    }

    public void warn (Member member, Guild guild, String reason) throws Exception {

        int warnPoints = getWarnCount(member);

        if ((warnPoints + 1) >= 3) { new Ban().ban(member, "3/3 warns"); }
        else addToDatabase(member.getId(), guild.getId(), reason);
    }

    public int getWarnCount (Member member) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        return (int) warns.count(eq("user_id", member.getId())); // TODO: Replace with countDocuments
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Member member = event.getOption("user").getAsMember();
        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();
        int warnPoints = getWarnCount(member);
        var eb = new EmbedBuilder()
                .setColor(Color.decode(Bot.config.get(event.getGuild()).getSlashCommand()
                        .getWarningColor()))
                .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();


        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

        try {
            warn(member, event.getGuild(), reason);
            return event.replyEmbeds(eb);
        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }
    }
}

