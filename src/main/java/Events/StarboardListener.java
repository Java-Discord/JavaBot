package Events;

import Other.Database;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.awt.*;

import static Events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class StarboardListener extends ListenerAdapter {


    void addToSB(GuildMessageReactionAddEvent event) {

        String gID = event.getGuild().getId();
        String cID = event.getChannel().getId();
        String mID = event.getMessageId();

        changeSBCBool(event.getGuild().getId(), event.getChannel().getId(), mID, true);

        Message msg = event.getChannel().retrieveMessageById(mID).complete();
        TextChannel sc = event.getGuild().getTextChannelById(Database.getConfigString(event, "starboard_cid"));

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(msg.getAuthor().getAsTag(), msg.getJumpUrl(), msg.getAuthor().getEffectiveAvatarUrl())
                .setColor(new Color(0x2F3136))
                .setDescription(msg.getContentRaw());

        try {

            Message.Attachment attachment = msg.getAttachments().get(0);

            if (attachment.isImage()) {

                eb.setImage(msg.getAttachments().get(0).getUrl());
            } else if (attachment.isVideo()) {

                eb.setImage(null);

                if (msg.getContentRaw().isBlank()) eb.setDescription(attachment.getUrl());
                else eb.setDescription(attachment.getUrl() + "\n\n" + eb.build().getDescription());
            }

            sc.sendMessage(Database.getConfigString(event, "starboard_emote")
                    + " " + getStarCount(gID, cID, mID) + " | " + msg.getTextChannel().getAsMention()).embed(eb.build())
                    .queue((message) -> {
                        String eMID = message.getId();
                        querySBDString(gID, cID, mID, "starboard_embed", eMID);
                    });

        } catch (IndexOutOfBoundsException e) {

            eb.setImage(null);

            sc.sendMessage(Database.getConfigString(event, "starboard_emote")
                    + " " + getStarCount(gID, cID, mID) + " | " + msg.getTextChannel().getAsMention()).embed(eb.build())
                    .queue((message) -> {
                        String eMID = message.getId();
                        querySBDString(gID, cID, mID, "starboard_embed", eMID);
                    });
        }
    }

    void updateSB(GuildMessageReactionAddEvent event, String cID, String mID) {

        TextChannel tc = event.getGuild().getTextChannelById(cID);

        Message msg = event.getGuild().getTextChannelById(Database.getConfigString(event, "starboard_cid"))
                .retrieveMessageById(getSBCString(event.getGuild().getId(), cID, mID, "starboard_embed")).complete();


        msg.editMessage(Database.getConfigString(event, "starboard_emote")
                + " " + getStarCount(event.getGuild().getId(), cID, mID) + " | " +
                tc.getAsMention()).queue();
    }

    public static void updateAllSBM(ReadyEvent event) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");
        MongoCursor<Document> it = collection.find().iterator();

        while (it.hasNext()) {

            JsonObject Root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();

            if (Root.get("isInSBC").getAsBoolean()) {

                String gID = Root.get("guild_id").getAsString();
                String cID = Root.get("channel_id").getAsString();
                String mID = Root.get("message_id").getAsString();

                Guild guild = event.getJDA().getGuildById(gID);

                /*System.out.println(Arrays.toString(guild.getTextChannelById(cID).retrieveMessageById(mID).complete()
                        .getReactions().stream().toArray()));*/

                guild.getTextChannelById(cID).retrieveMessageById(mID).queue(msg -> {
                            msg.getReactions().stream()
                                    .filter(msge -> msge.getReactionEmote().getName().equals("⭐")).iterator();

                        });

                        System.out.println(guild.getTextChannelById(cID).retrieveMessageById(mID).complete().getReactions());

                        }
            }

        }

    void increaseEmoteCount(String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        int i = getStarCount(gID, cID, mID);

        Document query = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document SetData = new Document();
        SetData.append("starcount", i + 1);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(query, update);

    }

    void decreaseEmoteCount(String gID, String cID, String mID) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("starboard_messages");

        int i = getStarCount(gID, cID, mID);

        Document query = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document SetData = new Document();
        SetData.append("starcount", i - 1);

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

        if (event.getReactionEmote().getName().equals(Database.getConfigString(event, "starboard_emote"))) {

            String gID = event.getGuild().getId();
            String cID = event.getChannel().getId();
            String mID = event.getMessageId();

            if (sbDocExists(gID, cID, mID)) {

                increaseEmoteCount(gID, cID, mID);
                if (getSBCBool(gID, cID, mID)) updateSB(event, cID, mID);
                else if (getStarCount(gID, cID, mID) >= 3) addToSB(event);

            } else {

                createSBDoc(gID, cID, mID);
            }
        }
    }

        /*@Override
        public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event){
            if (event.getMember().getUser().isBot()) return;

            // TBD!

            if (event.getReactionEmote().getName().equals(Database.configGetString(event, "starboard_emote"))) {

                String gID = event.getGuild().getId();
                String cID = event.getChannel().getId();
                String mID = event.getMessageId();

                if (sbDocExists(gID, cID, mID)) {

                    increaseEmoteCount(gID, cID, mID);
                    if (getSBCBool(gID, cID, mID)) updateSB(event, cID, mID);
                    else if (getStarCount(gID, cID, mID) >= 3) addToSB(event);

                } else {

                    createSBDoc(gID, cID, mID);
                }
            }

        }*/
    }
