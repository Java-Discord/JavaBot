package com.javadiscord.javabot.events;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class StarboardListener extends ListenerAdapter {

    void addToSB(Guild guild, MessageChannel channel, Message message) {
        String guildId = guild.getId();
        String channelId = channel.getId();
        String messageId = channel.getId();

        Database db = new Database();
        db.changeSBCBool(guildId, channelId, messageId, true);
        TextChannel sc = guild.getTextChannelById(db.getConfigString(guild, "other.starboard.starboard_cid"));

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("Jump to message", message.getJumpUrl())
                .setFooter(message.getAuthor().getAsTag(), message.getAuthor().getEffectiveAvatarUrl())
                .setColor(Constants.GRAY)
                .setDescription(message.getContentRaw());
        MessageAction msgAction = sc
                .sendMessage(db.getConfigString(guild, "other.starboard.starboard_emote") + " " +
                        db.getStarCount(guildId, channelId, messageId) + " | " + message.getTextChannel().getAsMention())
                .setEmbeds(eb.build());
        if (!message.getAttachments().isEmpty()) {
            try {
                Message.Attachment attachment = message.getAttachments().get(0);
                msgAction.addFile(attachment.retrieveInputStream().get(),
                        "attachment." + attachment.getFileExtension());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        msgAction.queue(sbMsg -> db.querySBDString(guildId, channelId, messageId, "starboard_embed", sbMsg.getId()));
    }

    void updateSB(Guild guild, String cID, String mID) {

        Database db = new Database();
        String gID = guild.getId();

        String sbcEmbedId = db.getSBCString(gID, cID, mID, "starboard_embed");
        if (sbcEmbedId == null) {
            return;
        }

        Message sbMsg = guild
                .getTextChannelById(db.getConfigString(guild, "other.starboard.starboard_cid"))
                .retrieveMessageById(sbcEmbedId)
                .complete();

        int starCount = db.getStarCount(gID, cID, mID);
        if (starCount > 0) {
            String starLevel = "starboard_emote";
            if (starCount > 10)
                starLevel = "starboard_emote2";
            if (starCount > 25)
                starLevel = "starboard_emote3";

            TextChannel tc = guild.getTextChannelById(cID);
            sbMsg.editMessage(db.getConfigString(guild, "other.starboard." + starLevel) + " "
                    + starCount + " | " + tc.getAsMention()).queue();
        } else {
            sbMsg.delete().queue();
            db.deleteSBMessage(gID, cID, mID);
        }
    }

    public void updateAllSBM(Guild guild) {

        String gID = guild.getId();
        MongoCollection<Document> collection = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages");
        MongoCursor<Document> it = collection.find(eq("guild_id", gID)).iterator();

        Database db = new Database();
        while (it.hasNext()) {
            Document doc = it.next();

            String cID = doc.getString("channel_id");
            String mID = doc.getString("message_id");

            Message msg = null;

            try {
                msg = guild.getTextChannelById(cID).retrieveMessageById(mID).complete();
            } catch (ErrorResponseException e) {
                if (doc.getBoolean("isInSBC").booleanValue()) {
                    db.getConfigChannel(guild, "other.starboard.starboard_cid")
                            .retrieveMessageById(doc.getString("starboard_embed"))
                            .complete()
                            .delete()
                            .queue();
                }
                collection.deleteOne(doc);
                continue;
            }

            String emote = db.getConfigString(guild, "other.starboard.starboard_emote");

            if (msg.getReactions().isEmpty()) {
                db.setEmoteCount(gID, cID, mID, 0);
                updateSB(guild, cID, mID);
            }

            int reactionCount = msg
                    .getReactions()
                    .stream()
                    .filter(r -> r.getReactionEmote().getName().equals(emote))
                    .findFirst()
                    .map(MessageReaction::getCount)
                    .orElse(0);
            
            db.setEmoteCount(gID, cID, mID, reactionCount);
            if (!db.getSBCBool(gID, cID, mID) && reactionCount >= 3) {
                addToSB(guild, guild.getTextChannelById(cID), msg);
            }
            if (db.getSBCString(gID, cID, mID, "starboard_embed") != null) {
                updateSB(guild, cID, mID);
            }
        }
        it.close();
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot())
            return;

        Database db = new Database();
        Guild guild = event.getGuild();
        if (!event.getReactionEmote().getName()
                .equals(db.getConfigString(guild, "other.starboard.starboard_emote")))
            return;

        String gID = guild.getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        if (db.sbDocExists(gID, cID, mID)) {

            int starCount = db.getStarCount(gID, cID, mID);
            db.setEmoteCount(gID, cID, mID, starCount + 1);
            if (db.getSBCBool(gID, cID, mID))
                updateSB(guild, cID, mID);
            else if (starCount >= 3)
                addToSB(guild, event.getChannel(),
                        event.getChannel().retrieveMessageById(event.getMessageId()).complete());

        } else {
            db.createSBDoc(gID, cID, mID);
        }
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (event.getUser().isBot())
            return;

        Database db = new Database();

        if (!event.getReactionEmote().getName()
                .equals(db.getConfigString(event.getGuild(), "other.starboard.starboard_emote")))
            return;

        String gID = event.getGuild().getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        if (db.sbDocExists(gID, cID, mID)) {

            int starCount = db.getStarCount(gID, cID, mID);
            db.setEmoteCount(gID, cID, mID, starCount - 1);
            if (db.getSBCBool(gID, cID, mID))
                updateSB(event.getGuild(), cID, mID);
            else if (starCount >= 3)
                addToSB(event.getGuild(), event.getChannel(),
                        event.getChannel().retrieveMessageById(event.getMessageId()).complete());

        } else {
            db.createSBDoc(gID, cID, mID);
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        MongoCollection<Document> collection = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages");

        Document doc = collection.find(eq("message_id", event.getMessageId())).first();
        if (doc == null)
            return;
        
        String var = doc.getString("starboard_embed");

        new Database().getConfigChannel(event.getGuild(), "other.starboard.starboard_cid")
                .retrieveMessageById(var)
                .complete()
                .delete()
                .queue();
        collection.deleteOne(doc);
    }
}
