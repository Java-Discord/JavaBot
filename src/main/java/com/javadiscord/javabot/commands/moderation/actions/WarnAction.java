package com.javadiscord.javabot.commands.moderation.actions;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import com.javadiscord.javabot.other.TimeUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class WarnAction implements ActionHandler {

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
    public void handle(Object ev, Member member, User author, String reason) {

        Guild guild = null;
        boolean slash = false;

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            guild = event.getGuild();
            slash = true;
        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
            slash = false;
        }

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> warns = database.getCollection("warns");

        int warnPoints = (int) warns.count(eq("user_id", member.getId()));

        var eb = new EmbedBuilder()
                .setColor(Constants.YELLOW)
                .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        if (slash) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
            event.replyEmbeds(eb).queue();
        } else {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
            event.getChannel().sendMessageEmbeds(eb).queue();
        }

        try {

            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();
            Misc.sendToLog(guild, eb);

            if ((warnPoints + 1) >= 3) {

                if (slash) {
                    net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                    new BanAction().handle(event, member, event.getJDA().getSelfUser(), "3/3 warns");
                } else {
                    net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
                    new BanAction().handle(event, member, event.getJDA().getSelfUser(), "3/3 warns");
                }

            } else addToDatabase(member.getId(), guild.getId(), reason);

        } catch (Exception e) {

            if (slash) {
                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", author)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            } else {
                net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
                event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", author)).queue();
            }
        }
    }
}
