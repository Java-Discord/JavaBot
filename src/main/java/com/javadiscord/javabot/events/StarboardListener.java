package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class StarboardListener extends ListenerAdapter {

    void addToSB(Guild guild, MessageChannel channel, Message message) {

        changeSBCBool(guild.getId(), channel.getId(), message.getId(), true);
        TextChannel sc = guild.getTextChannelById(new Database().getConfigString(guild, "other.starboard.starboard_cid"));

        var eb = new EmbedBuilder()
                .setAuthor("Jump to message", message.getJumpUrl())
                .setFooter(message.getAuthor().getAsTag(), message.getAuthor().getEffectiveAvatarUrl())
                .setColor(Constants.GRAY)
                .setDescription(message.getContentRaw());

             if (!message.getAttachments().isEmpty()) {

                 try {
                     Message.Attachment attachment = message.getAttachments().get(0);

                     sc.sendMessage(new Database().getConfigString(guild, "other.starboard.starboard_emote")
                             + " " + getStarCount(guild.getId(), channel.getId(), message.getId()) + " | " + message.getTextChannel().getAsMention()).setEmbeds(eb.build())
                             .addFile(attachment.retrieveInputStream().get(), "attachment." + attachment.getFileExtension())
                             .queue((msg) -> {
                                 String eMID = msg.getId();
                                 querySBDString(guild.getId(), channel.getId(), message.getId(), "starboard_embed", eMID);
                             });
                 } catch (Exception e) { e.printStackTrace(); }

             } else {

                 sc.sendMessage(new Database().getConfigString(guild, "other.starboard.starboard_emote")
                         + " " + getStarCount(guild.getId(), channel.getId(), message.getId()) + " | " + message.getTextChannel().getAsMention()).setEmbeds(eb.build())
                         .queue((msg) -> {
                             String eMID = msg.getId();
                             querySBDString(guild.getId(), channel.getId(), message.getId(), "starboard_embed", eMID);
                         });
             }
        }

    void updateSB(Guild guild, String cID, String mID) {

        TextChannel tc = guild.getTextChannelById(cID);
        Message msg = null;

        if (!(getSBCString(guild.getId(), cID, mID, "starboard_embed").equals("null"))) {
            msg = guild.getTextChannelById(new Database().getConfigString(guild, "other.starboard.starboard_cid"))
                    .retrieveMessageById(getSBCString(guild.getId(), cID, mID, "starboard_embed")).complete();
        }

        if (getStarCount(guild.getId(), cID, mID) > 0) {

            msg.editMessage(new Database().getConfigString(guild, "other.starboard.starboard_emote")
                    + " " + getStarCount(guild.getId(), cID, mID) + " | " +
                    tc.getAsMention()).queue();
        } else {
            if (!(msg == null)) msg.delete().queue();

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("starboard_messages");

            BasicDBObject criteria = new BasicDBObject("guild_id", guild.getId())
                    .append("channel_id", cID)
                    .append("message_id", mID);

            collection.deleteOne(criteria);
        }
    }

    public void updateAllSBM(ReadyEvent event) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");
        MongoCursor<Document> it = collection.find().iterator();

        while (it.hasNext()) {

            Document doc = it.next();
            JsonObject root = JsonParser.parseString(doc.toJson()).getAsJsonObject();

            String gID = root.get("guild_id").getAsString();
            String cID = root.get("channel_id").getAsString();
            String mID = root.get("message_id").getAsString();

            Message msg = null;
            Guild guild = event.getJDA().getGuildById(gID);

            try {
                msg = guild.getTextChannelById(cID).retrieveMessageById(mID).complete();
            } catch (ErrorResponseException e) {

                if (root.get("isInSBC").getAsBoolean()) {
                    new Database().getConfigChannel(guild, "other.starboard.starboard_cid")
                            .retrieveMessageById(root.get("starboard_embed").getAsString()).complete().delete().queue();
                }
                collection.deleteOne(doc);
                continue;
            }

            String emote = new Database().getConfigString(guild, "other.starboard.starboard_emote");

            if (msg.getReactions().isEmpty()) {
                setEmoteCount(0, gID, cID, mID);
                updateSB(event.getJDA().getGuildById(gID), cID, mID);
            }

            for (int i = msg.getReactions().size(); i > 0; i--) {

                if (!msg.getReactions().get(i - 1).getReactionEmote().getName().equals(emote)) return;

                setEmoteCount(msg.getReactions().get(i - 1).getCount(), gID, cID, mID);
                if (msg.getReactions().get(i - 1).getCount() >= 3 && !getSBCBool(gID, cID, mID)) addToSB(guild, guild.getTextChannelById(cID), msg);
                if (!(getSBCString(guild.getId(), cID, mID, "starboard_embed").equals("null"))) updateSB(event.getJDA().getGuildById(gID), cID, mID);
            }
        }
    }

    void setEmoteCount(int value, String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        Document query = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document SetData = new Document();
        SetData.append("starcount", value);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(query, update);
    }

    void changeSBCBool(String gID, String cID, String mID, boolean sbc) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        Document query = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document SetData = new Document();
        SetData.append("isInSBC", sbc);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(query, update);
    }

    void querySBDString(String gID, String cID, String mID, String value, String newValue) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        Document query = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document SetData = new Document();
        SetData.append(value, newValue);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(query, update);
    }

    int getStarCount(String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        int sc;

        try {
            String doc = collection.find(criteria).first().toJson();
            JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
            sc = Root.get("starcount").getAsInt();
        } catch (NullPointerException e) {

            createSBDoc(gID, cID, mID);
            sc = 0;
        }

        return sc;

    }

    boolean getSBCBool(String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        String doc = collection.find(criteria).first().toJson();
        JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();

        return Root.get("isInSBC").getAsBoolean();

    }

    String getSBCString(String gID, String cID, String mID, String value) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        String doc = collection.find(criteria).first().toJson();
        JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();

        return Root.get(value).getAsString();

    }

    void createSBDoc(String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        Document doc = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID)
                .append("starcount", 1)
                .append("isInSBC", false)
                .append("starboard_embed", "null");

        collection.insertOne(doc);
    }

    boolean sbDocExists(String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        boolean exists;

        try {
            String doc = collection.find(eq("guild_id", gID)).first().toJson();

            JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
            String var = Root.get("guild_id").getAsString();
            exists = true;

        } catch (Exception e) {
            exists = false;
        }

        return exists;
    }


    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getMember().getUser().isBot()) return;

        if (!event.getReactionEmote().getName().equals(new Database().getConfigString(event.getGuild(), "other.starboard.starboard_emote")))
            return;

        String gID = event.getGuild().getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        if (sbDocExists(gID, cID, mID)) {

            setEmoteCount(getStarCount(gID, cID, mID) + 1, gID, cID, mID);
            if (getSBCBool(gID, cID, mID)) updateSB(event.getGuild(), cID, mID);
            else if (getStarCount(gID, cID, mID) >= 3)
                addToSB(event.getGuild(), event.getChannel(), event.getChannel().retrieveMessageById(event.getMessageId()).complete());

        } else {
            createSBDoc(gID, cID, mID);
        }
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (event.getMember().getUser().isBot()) return;

        if (!event.getReactionEmote().getName().equals(new Database().getConfigString(event.getGuild(), "other.starboard.starboard_emote"))) return;

        String gID = event.getGuild().getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        if (sbDocExists(gID, cID, mID)) {

            setEmoteCount(getStarCount(gID, cID, mID) - 1, gID, cID, mID);
            if (getSBCBool(gID, cID, mID)) updateSB(event.getGuild(), cID, mID);
            else if (getStarCount(gID, cID, mID) >= 3)
                addToSB(event.getGuild(), event.getChannel(), event.getChannel().retrieveMessageById(event.getMessageId()).complete());

        } else { createSBDoc(gID, cID, mID); }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        Document doc = collection.find(eq("message_id", event.getMessageId())).first();
        if (doc == null) return;

        JsonObject Root = JsonParser.parseString(doc.toJson()).getAsJsonObject();
        String var = Root.get("starboard_embed").getAsString();

        new Database().getConfigChannel(event.getGuild(), "other.starboard.starboard_cid").retrieveMessageById(var).complete().delete().queue();
        collection.deleteOne(doc);
    }
}
