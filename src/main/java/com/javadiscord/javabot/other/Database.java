package com.javadiscord.javabot.other;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

public class Database {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Database.class);

    private static final Map<String, String[]> DB_COLS = new HashMap<>();
    static {
        DB_COLS.put("userdata", new String[] { "potential_bot_list", "users", "warns" });
        DB_COLS.put("other", new String[] { "config", "customcommands", "expert_questions",
                "open_submissions", "reactionroles", "starboard_messages", "submission_messages" });
    }

    List<String> getDatabases(MongoClient mongoClient) {
        return mongoClient.listDatabaseNames().into(new ArrayList<>());
    }

    List<String> getCollections(MongoClient mongoClient, String database) {
        return mongoClient.getDatabase(database).listCollectionNames().into(new ArrayList<>());
    }

    public void databaseCheck(MongoClient mongoClient, List<Guild> guilds) {
        for (Map.Entry<String, String[]> entry : DB_COLS.entrySet()) {
            String dbName = entry.getKey();
            String[] collection = entry.getValue();
            if (!getDatabases(mongoClient).contains(dbName)) {
                logger.warn("MongoDB: Missing Database: {}. Creating one now.", dbName);
            }

            for (var cols : collection) {
                if (!getCollections(mongoClient, dbName).contains(cols)) {
                    logger.warn("MongoDB: Missing Collection in Database {}: {}. Creating one now.", dbName, cols);
                    mongoClient.getDatabase(dbName).createCollection(cols);
                }
            }
        }
        guilds.forEach(this::insertGuildDoc);
    }

    public void deleteOpenSubmissions(Guild guild) {
        MongoCollection<Document> collection = mongoClient
                .getDatabase("other")
                .getCollection("open_submissions");

        for (var document : collection.find(eq("guild_id", guild.getId()))) {
            String messageId = document.getString("message_id");
            String userId = document.getString("user_id");

            User user = preferredGuild.retrieveMemberById(userId).complete().getUser();
            Message msg = user.openPrivateChannel().complete().retrieveMessageById(messageId).complete();

            msg.editMessageEmbeds(msg.getEmbeds().get(0))
                    .setActionRows(ActionRow.of(
                            Button.danger("dm-submission:canceled:" + user.getId(), "Process canceled").asDisabled()))
                    .queue();

            collection.deleteOne(document);
        }
    }

    public Document userDoc(Member member) {
        return userDoc(member.getUser());
    }

    public Document versionDoc(JDA jda) {
        return new Document("name", jda.getSelfUser().getAsTag())
                .append("version", "v00-00.00");
    }

    public Document userDoc(User user) {
        return new Document("tag", user.getAsTag())
                .append("discord_id", user.getId())
                .append("qotwpoints", 0);
    }

    public Document guildDoc(Guild guild) {

        Document av = new Document("avX", 75)
                .append("avY", 100)
                .append("avH", 400)
                .append("avW", 400);

        Document wi = new Document("avatar", av)
                .append("imgW", 1920)
                .append("imgH", 600)
                .append("overlayURL",
                        "https://cdn.discordapp.com/attachments/744899463591624815/827303132098461696/WelcomeOverlay_NoShadow.png")
                .append("bgURL",
                        "https://cdn.discordapp.com/attachments/744899463591624815/840928322661122048/WelcomeBG.png")
                .append("primCol", "16777215")
                .append("secCol", "16720173");

        Document ws = new Document("image", wi)
                .append("join_msg", "None")
                .append("leave_msg", "None")
                .append("welcome_cid", "None")
                .append("welcome_status", false);

        Document channels = new Document("report_cid", "None")
                .append("log_cid", "None")
                .append("suggestion_cid", "None")
                .append("submission_cid", "None")
                .append("jam_announcement_cid", "None")
                .append("jam_vote_cid", "None");

        Document roles = new Document("mute_rid", guild.getId())
                .append("staff_rid", guild.getId())
                .append("jam_admin_rid", guild.getId())
                .append("jam_ping_rid", guild.getId());

        Document stats = new Document("stats_cid", "None")
                .append("stats_text", "None");

        Document qotw = new Document("dm-qotw", false);

        Document lock = new Document("lock_status", false)
                .append("lock_count", 0);

        Document sb = new Document("starboard_cid", "None")
                .append("starboard_emote", "⭐")
                .append("starboard_emote2", "\uD83C\uDF1F")
                .append("starboard_emote3", "\uD83C\uDF20");

        Document other = new Document("stats_category", stats)
                .append("qotw", qotw)
                .append("server_lock", lock)
                .append("starboard", sb);

        return new Document("name", guild.getName())
                .append("guild_id", guild.getId())
                .append("welcome_system", ws)
                .append("channels", channels)
                .append("roles", roles)
                .append("other", other);
    }

    public boolean guildDocExists(Guild guild) {
        if (guild == null)
            return false;

        return mongoClient.getDatabase("other")
                .getCollection("config")
                .find(eq("guild_id", guild.getId()))
                .first() != null;
    }

    public void insertGuildDoc(Guild guild) {
        mongoClient.getDatabase("other")
                .getCollection("config")
                .insertOne(guildDoc(guild));
        logger.info("Added Database entry for Guild \"{}\" ({})", guild.getName(), guild.getId());
    }

    public void queryMember(String memberID, String varName, Object newValue) {
        Document setData = new Document(varName, newValue);
        Document update = new Document("$set", setData);

        Document query = new Document("discord_id", memberID);
        mongoClient.getDatabase("userdata")
                .getCollection("users")
                .updateOne(query, update);
    }

    public String getMemberString(User user, String varName) {
        MongoCollection<Document> collection = mongoClient
                .getDatabase("userdata")
                .getCollection("users");
        Document userDoc = collection
                .find(eq("discord_id", user.getId()))
                .first();
        if (userDoc == null) {
            collection.insertOne(userDoc(user));
            return "0";
        }
        return userDoc.get(varName, "0");
    }

    public int getMemberInt(Member member, String varName) {
        MongoCollection<Document> collection = mongoClient
                .getDatabase("userdata")
                .getCollection("users");
        Document userDoc = collection
                .find(eq("discord_id", member.getId()))
                .first();
        if (userDoc == null) {
            collection.insertOne(userDoc(member));
            return 0;
        }
        return userDoc.getInteger(varName, 0);
    }

    public void queryConfig(String guildID, String path, Object newValue) {
        BasicDBObject setData = new BasicDBObject(path, newValue);
        BasicDBObject update = new BasicDBObject("$set", setData);

        Document query = new Document("guild_id", guildID);
        mongoClient.getDatabase("userdata")
                .getCollection("users")
                .updateOne(query, update);
    }

    private <T> T getConfig(Guild guild, String path, T defaultValue) {
        try {
            Document doc = mongoClient
                    .getDatabase("other")
                    .getCollection("config")
                    .find(eq("guild_id", guild.getId()))
                    .first();
            String[] splittedPath = path.split("\\.");
            int pathLen = splittedPath.length - 1;

            for (int i = 0; i < pathLen; ++i) {
                doc = doc.get(splittedPath[i], Document.class);
            }
            return doc.get(splittedPath[pathLen], defaultValue);
        } catch (Exception e) {
            if (!guildDocExists(guild)) {
                insertGuildDoc(guild);
            }
            return defaultValue;
        }
    }

    public String getConfigString(Guild guild, String path) {
        return getConfig(guild, path, "None");
    }

    public int getConfigInt(Guild guild, String path) {
        return getConfig(guild, path, 0);
    }

    public boolean getConfigBoolean(Guild guild, String path) {
        return getConfig(guild, path, false);
    }

    public TextChannel getConfigChannel(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getTextChannelById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Role getConfigRole(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getRoleById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getConfigChannelAsMention(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getTextChannelById(id).getAsMention();
        } catch (NumberFormatException e) {
            return "None";
        }
    }

    public String getConfigRoleAsMention(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getRoleById(id).getAsMention();
        } catch (NumberFormatException e) {
            return "None";
        }
    }

    private void querySB(String guildId, String channelId, String messageId, String prop, Object newValue) {
        Document setData = new Document(prop, newValue);
        Document update = new Document("$set", setData);

        Document query = new Document("guild_id", guildId)
                .append("channel_id", channelId)
                .append("message_id", messageId);
        mongoClient.getDatabase("other")
                .getCollection("starboard_messages")
                .updateOne(query, update);
    }

    public void querySBDString(String gID, String cID, String mID, String value, String newValue) {
        querySB(gID, cID, mID, value, newValue);
    }

    public void changeSBCBool(String gID, String cID, String mID, boolean sbc) {
        querySB(gID, cID, mID, "isInSBC", sbc);
    }

    public void setEmoteCount(String gID, String cID, String mID, int value) {
        querySB(gID, cID, mID, "starcount", value);
    }

    public boolean sbDocExists(String gID, String cID, String mID) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);
        Document first = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages")
                .find(criteria)
                .first();
        return first != null && first.getString("guild_id") != null;
    }

    public void createSBDoc(String gID, String cID, String mID) {
        Document doc = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID)
                .append("starcount", 1)
                .append("isInSBC", false)
                .append("starboard_embed", "null");

        mongoClient.getDatabase("other")
                .getCollection("starboard_messages")
                .insertOne(doc);
    }

    public int getStarCount(String gID, String cID, String mID) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document first = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages")
                .find(criteria)
                .first();

        if (first == null) {
            new Database().createSBDoc(gID, cID, mID);
            return 0;
        }
        return first.getInteger("starcount", 0);
    }

    public boolean getSBCBool(String gID, String cID, String mID) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);
        Document first = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages")
                .find(criteria)
                .first();
        return first != null && first.getBoolean("isInSBC");
    }

    public String getSBCString(String gID, String cID, String mID, String value) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);
        Document first = mongoClient.getDatabase("other")
                .getCollection("starboard_messages")
                .find(criteria)
                .first();
        return first == null ? null : first.getString(value);
    }

    public void deleteSBMessage(String gID, String cID, String mID) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);
        mongoClient.getDatabase("other")
                .getCollection("starboard_messages")
                .deleteOne(criteria);
    }
}
