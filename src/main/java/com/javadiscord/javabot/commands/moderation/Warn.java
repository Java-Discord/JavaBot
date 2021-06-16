package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.*;
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
import java.util.UUID;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Warn {

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

    public static void warn(Member member, String reason, String modTag, Object ev) {

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

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            tc = event.getChannel();
            guildID = event.getGuild().getId();
            selfUser = event.getJDA().getSelfUser();
            tc.sendMessage(eb).queue();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            tc = event.getTextChannel();
            guildID = event.getGuild().getId();
            selfUser = event.getJDA().getSelfUser();
            event.replyEmbeds(eb).queue();
        }

        try {

            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();
            Misc.sendToLog(ev, eb);

            if ((warnPoints + 1) >= 3) Ban.ban(member, "3/3 warns", selfUser.getAsTag(), ev);
            else addToDatabase(member.getId(), guildID, reason);

        } catch (HierarchyException e) {

            if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.hierarchyError(ev)).setEphemeral(Constants.ERR_EPHEMERAL).queue();

            } else { tc.sendMessage(Embeds.hierarchyError(ev)).queue(); }
        }

    }

    public static void execute(SlashCommandEvent event, Member member, User author, String reason) {
        if (event.getMember().hasPermission(Permission.KICK_MEMBERS)) {

            warn(member, reason, author.getAsTag(), event);

        } else { event.replyEmbeds(Embeds.permissionError("KICK_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }
}

