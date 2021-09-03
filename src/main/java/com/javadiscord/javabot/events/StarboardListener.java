package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class StarboardListener extends ListenerAdapter {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StarboardListener.class);

    void addToSB(Guild guild, MessageChannel channel, Message message, int starCount) {

        String gID = guild.getId();
        String cID = channel.getId();
        String mID = message.getId();

        Database db = new Database();
        var config = Bot.config.get(guild).getStarBoard();
        TextChannel sc = config.getChannel();

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("Jump to message", message.getJumpUrl())
                .setFooter(message.getAuthor().getAsTag(), message.getAuthor().getEffectiveAvatarUrl())
                .setColor(Constants.GRAY)
                .setDescription(message.getContentRaw());

        MessageAction msgAction = sc
                .sendMessage(db.getConfigString(guild, "other.starboard.starboard_emote") + " " +
                        starCount + " | " + message.getTextChannel().getAsMention())
                .setEmbeds(eb.build());

        if (!message.getAttachments().isEmpty()) {
            try {
                Message.Attachment attachment = message.getAttachments().get(0);
                msgAction.addFile(attachment.retrieveInputStream().get(),
                        mID + "." + attachment.getFileExtension());
            } catch (Exception e) { e.printStackTrace(); }
        }
        msgAction.queue(sbMsg -> db.createStarboardDoc(gID, cID, mID, sbMsg.getId()));
    }

    void removeFromSB(Guild guild, String mID) {

        MongoCollection<Document> collection = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages");

        Document doc = collection.find(eq("message_id", mID)).first();
        if (doc == null) return;

        String var = doc.getString("starboard_embed");

        Bot.config.get(guild).getStarBoard().getChannel()
                .retrieveMessageById(var)
                .complete()
                .delete()
                .queue();

        collection.deleteOne(doc);
    }

    void updateSB(Guild guild, String cID, String mID, int reactionCount) {

        Database db = new Database();
        String gID = guild.getId();

        String sbcEmbedId = db.getStarboardChannelString(gID, cID, mID, "starboard_embed");
        if (sbcEmbedId == null) return;

        Message sbMsg;

        try {
            sbMsg = Bot.config.get(guild).getStarBoard().getChannel()
                .retrieveMessageById(sbcEmbedId).complete();
        } catch (Exception e) { return; }

        if (sbMsg == null) return;

        if (reactionCount > 0) {
            String starLevel = "starboard_emote";
            if (reactionCount > 10)
                starLevel = "starboard_emote2";
            if (reactionCount > 25)
                starLevel = "starboard_emote3";

            TextChannel tc = guild.getTextChannelById(cID);
            sbMsg.editMessage(db.getConfigString(guild, "other.starboard." + starLevel) + " "
                    + reactionCount + " | " + tc.getAsMention()).queue();

        } else { removeFromSB(guild, mID); }
    }

    public void updateAllSBM(Guild guild) {
        logger.info("[{}] Updating Starboard Messages", guild.getName());

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

            Message msg;

            try { msg = guild.getTextChannelById(cID).retrieveMessageById(mID).complete();
            } catch (ErrorResponseException e) { collection.deleteOne(doc); continue; }

            String emote = db.getConfigString(guild, "other.starboard.starboard_emote");
            if (msg.getReactions().isEmpty()) updateSB(guild, cID, mID, 0);

            int reactionCount = msg
                    .getReactions()
                    .stream()
                    .filter(r -> r.getReactionEmote().getName().equals(emote))
                    .findFirst()
                    .map(MessageReaction::getCount)
                    .orElse(0);

            if (!db.isMessageOnStarboard(gID, cID, mID) && reactionCount >= 3) {
                addToSB(guild, guild.getTextChannelById(cID), msg, reactionCount);
            }

            else if (db.getStarboardChannelString(gID, cID, mID, "starboard_embed") != null) {
                updateSB(guild, cID, mID, reactionCount);
            }
        }
        it.close();
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;

        Database db = new Database();
        String sbEmote = db.getConfigString(event.getGuild(), "other.starboard.starboard_emote");

        if (!event.getReactionEmote().getName().equals(sbEmote)) return;

        String gID = event.getGuild().getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        Message message = event.getChannel().retrieveMessageById(mID).complete();

        int reactionCount = message
                .getReactions()
                .stream()
                .filter(r -> r.getReactionEmote().getName().equals(sbEmote))
                .findFirst()
                .map(MessageReaction::getCount)
                .orElse(0);

        if (db.isMessageOnStarboard(gID, cID, mID)) updateSB(event.getGuild(), cID, mID, reactionCount);
        else if (reactionCount >= 3) addToSB(event.getGuild(), event.getChannel(), message, reactionCount);
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (event.getUser().isBot()) return;

        Database db = new Database();
        String sbEmote = db.getConfigString(event.getGuild(), "other.starboard.starboard_emote");

        if (!event.getReactionEmote().getName().equals(sbEmote)) return;

        String gID = event.getGuild().getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        Message message = event.getChannel().retrieveMessageById(mID).complete();

        int reactionCount = message
            .getReactions()
            .stream()
            .filter(r -> r.getReactionEmote().getName().equals(sbEmote))
            .findFirst()
            .map(MessageReaction::getCount)
            .orElse(0);

            if (db.isMessageOnStarboard(gID, cID, mID)) updateSB(event.getGuild(), cID, mID, reactionCount);
            else if (reactionCount >= 3) addToSB(event.getGuild(), event.getChannel(), message, reactionCount);
            else if (reactionCount == 0) removeFromSB(event.getGuild(), mID);
     }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        removeFromSB(event.getGuild(), event.getMessageId());
    }
}
