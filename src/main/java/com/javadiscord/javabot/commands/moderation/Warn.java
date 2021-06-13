package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import com.mongodb.client.MongoCollection;
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

import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Warn {

    public static void warn(Member member, String reason, String modTag, Object ev) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        int warnPoints = Database.getMemberInt(collection, member, "warns");
        Database.queryMemberInt(member.getId(), "warns", warnPoints + 1);

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

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            tc = event.getChannel();
            selfUser = event.getJDA().getSelfUser();
            tc.sendMessage(eb).queue();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            tc = event.getTextChannel();
            selfUser = event.getJDA().getSelfUser();
            event.replyEmbeds(eb).queue();
        }

        try {

            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();
            Misc.sendToLog(ev, eb);

            if ((warnPoints + 1) >= 3) {

                Database.queryMemberInt(member.getId(), "warns", 0);
                Ban.ban(member, "3/3 warns", selfUser.getAsTag(), ev);
            }

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

