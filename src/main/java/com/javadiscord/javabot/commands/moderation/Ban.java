package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.bson.Document;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Ban {

    public static void ban(Member member, String reason, String modTag, Object ev){

        var eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.RED)
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + modTag + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        TextChannel tc = null;

        try {

            member.ban(6, reason).queueAfter(3, TimeUnit.SECONDS);

            Warn.deleteAllDocs(member.getId());

            if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
                net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

                tc = event.getChannel();
                tc.sendMessage(eb).queue();
            }

            if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

                tc = event.getTextChannel();
                if (reason.equalsIgnoreCase("3/3 warns")) tc.sendMessage(eb).queue();
                else event.replyEmbeds(eb).queue();
                }

            Misc.sendToLog(ev, eb);
            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();


        } catch (HierarchyException e) {

            if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {

                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.hierarchyError(ev)).setEphemeral(Constants.ERR_EPHEMERAL).queue();

            } else { tc.sendMessage(Embeds.hierarchyError(ev)).queue(); }

        } catch (NullPointerException | NumberFormatException e) {

            e.printStackTrace();

            if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {

                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", ev)).setEphemeral(Constants.ERR_EPHEMERAL).queue();

            } else { tc.sendMessage(Embeds.emptyError("```" + e.getMessage() + "```", ev)); }

        }
    }

    public static void execute(SlashCommandEvent event, Member member, User author, String reason) {
        if (event.getMember().hasPermission(Permission.BAN_MEMBERS)) {

                ban(member, reason, author.getAsTag(), event);

            } else { event.replyEmbeds(Embeds.permissionError("BAN_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
        }
    }