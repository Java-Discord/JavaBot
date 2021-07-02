package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import com.javadiscord.javabot.other.TimeUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Warn implements SlashCommandHandler {

    public static void addToDatabase(String memID, String guildID, String reason) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        Document doc = new Document("guild_id", guildID)
                .append("user_id", memID)
                .append("date", LocalDateTime.now().format(TimeUtils.STANDARD_FORMATTER))
                .append("reason", reason);

        warns.insertOne(doc);
    }

    public static void deleteAllDocs(String memID) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");
        MongoCursor<Document> it = warns.find(eq("user_id", memID)).iterator();

        while (it.hasNext()) {

            warns.deleteOne(it.next());
        }
    }

    @Override
    public void handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.replyEmbeds(Embeds.permissionError("KICK_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }
        Member member = event.getOption("user").getAsMember();
        String reason;
        try {
            reason = event.getOption("reason").getAsString();
        } catch (NullPointerException e) {
            reason = "None";
        }
        String modTag = event.getUser().getAsTag();

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        int warnPoints = (int) warns.count(eq("user_id", member.getId()));

        var eb = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
            .setColor(Constants.YELLOW)
            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
            .addField("Moderator", "```" + modTag + "```", true)
            .addField("ID", "```" + member.getId() + "```", false)
            .addField("Reason", "```" + reason + "```", false)
            .setFooter("ID: " +  member.getId())
            .setTimestamp(new Date().toInstant())
            .build();

        TextChannel tc = null;
        SelfUser selfUser = null;
        String guildID = null;
            guildID = event.getGuild().getId();
            selfUser = event.getJDA().getSelfUser();
            event.replyEmbeds(eb).queue();

        try {

            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();
            Misc.sendToLog(event, eb);

            if ((warnPoints + 1) >= 3) {
//                Ban.ban(member, "3/3 warns", selfUser.getAsTag(), ev);
                // TODO: New method for 3/3 warns ban, or extract ban logic out of handler.
            } else {
                addToDatabase(member.getId(), guildID, reason);
            }

        } catch (HierarchyException e) {
            event.replyEmbeds(Embeds.hierarchyError(event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}

