package com.javadiscord.javabot.commands.reaction_roles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.bson.Document;

import java.awt.*;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class ReactionRoles {

    public static void list(SlashCommandEvent event) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        StringBuilder sb = new StringBuilder();
        MongoCursor<Document> it = collection.find(eq("guild_id", event.getGuild().getId())).iterator();

        for (int i = 1; it.hasNext(); i++) {

            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String channelID = root.get("channel_id").getAsString();
            String messageID = root.get("message_id").getAsString();
            String roleID = root.get("role_id").getAsString();
            String emoteName = root.get("emote").getAsString();

            sb.append("#ReactionRole" + i +
                    "\n[CID] " + channelID +
                    "\n[MID] " + messageID +
                    "\n[RID] " + roleID +
                    "\n[EmoteName] " + emoteName + "\n\n");
        }

        String description;

        if (sb.length() > 0) {
            description = "```css\n" + sb + "```";
        } else {
            description = "```No Reaction Roles created yet```";
        }

            var e = new EmbedBuilder()
                    .setTitle("Reaction Role List")
                    .setDescription(description)
                    .setColor(new Color(0x2F3136))
                    .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                    .setTimestamp(new Date().toInstant())
                    .build();

            event.replyEmbeds(e).queue();
        }

    public static void create(SlashCommandEvent event, MessageChannel channel, String mID, String emote, Role role) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            boolean validEmote = false, integratedEmote = false;
            String emoteName = null;

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("reactionroles");

                if (emote.length() < 24) {
                    emoteName = emote;
                    integratedEmote = true;
                    validEmote = true;
                }

                if (!integratedEmote) {
                    emoteName = emote.substring(2, emote.length() - 20);

                    if (emoteName.startsWith(":")) emoteName = emoteName.substring(1);

                    try {

                        if (event.getGuild().getEmotes().contains(event.getGuild().getEmotesByName(emoteName, false).get(0))) {
                            validEmote = true;

                        } else {

                            event.replyEmbeds(Embeds.emptyError("```Please provide a valid Emote.```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
                            return;
                        }

                    } catch (IndexOutOfBoundsException e) {

                        event.replyEmbeds(Embeds.emptyError("```Please provide a valid Emote.```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
                        return;
                    }
                }

                BasicDBObject criteria = new BasicDBObject()
                        .append("guild_id", event.getGuild().getId())
                        .append("channel_id", channel.getId())
                        .append("message_id", mID)
                        .append("emote", emoteName);

                if (collection.find(criteria).first() == null) {

                    if (validEmote) {
                        if (integratedEmote) channel.addReactionById(mID, emoteName).complete();
                        else channel.addReactionById(mID, event.getGuild().getEmotesByName(emoteName, false).get(0)).complete();
                    }

                    Document doc = new Document()
                            .append("guild_id", event.getGuild().getId())
                            .append("channel_id", channel.getId())
                            .append("message_id", mID)
                            .append("role_id", role.getId())
                            .append("emote", emoteName);

                    collection.insertOne(doc);

                    var e = new EmbedBuilder()
                            .setTitle("Reaction Role created")
                            .addField("Channel", "<#" + channel.getId() + ">", true)
                            .addField("Role", role.getAsMention(), true)
                            .addField("Emote", "``" + emoteName + "``", true)
                            .addField("MessageID", "```" + mID + "```", false)
                            .setColor(Constants.GRAY)
                            .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                            .setTimestamp(new Date().toInstant())
                            .build();

                    event.replyEmbeds(e).setEphemeral(true).queue();
                    Misc.sendToLog(event, e);

                } else {
                    event.replyEmbeds(Embeds.emptyError("A Reaction Role on message ``" + mID + "`` with emote ``" + emoteName + "`` already exists.", event))
                            .setEphemeral(Constants.ERR_EPHEMERAL).queue();
                }

        } else { event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }

    public static void delete(SlashCommandEvent event, String mID, String emote) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

        boolean validEmote = false, integratedEmote = false;
        String emoteName = null;

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        if (emote.length() < 24) {
            emoteName = emote;
            integratedEmote = true;
            validEmote = true;
        }

        if (!integratedEmote) {
            emoteName = emote.substring(2, emote.length() - 20);

            if (emoteName.startsWith(":")) emoteName = emoteName.substring(1);

            try {
                if (event.getGuild().getEmotes().contains(event.getGuild().getEmotesByName(emoteName, false).get(0))) { validEmote = true; }

                else { event.replyEmbeds(Embeds.emptyError("```Please provide a valid Emote.```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); return; }
            } catch (IndexOutOfBoundsException e) { event.replyEmbeds(Embeds.emptyError("```Please provide a valid Emote.```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); return; }
        }


        BasicDBObject criteria = new BasicDBObject()
                .append("guild_id", event.getGuild().getId())
                .append("message_id", mID)
                .append("emote", emoteName);

        String doc = collection.find(criteria).first().toJson();
        JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
        mID = Root.get("channel_id").getAsString();

        if (validEmote) {
            if (integratedEmote) { event.getGuild().getTextChannelById(mID).removeReactionById(mID, emoteName).complete(); }
            else { event.getGuild().getTextChannelById(mID).removeReactionById(mID, event.getGuild().getEmotesByName(emoteName, false).get(0)).complete(); }
        }

        collection.deleteOne(criteria);

        var e = new EmbedBuilder()
                .setTitle("Reaction Role removed")
                .addField("Emote", "```" + emoteName + "```", true)
                .addField("MessageID", "```" + mID + "```", true)
                .setColor(new Color(0x2F3136))
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant())
                .build();

        event.replyEmbeds(e).queue();

        } else { event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }
}
