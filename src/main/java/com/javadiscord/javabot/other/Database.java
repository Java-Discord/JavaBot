package com.javadiscord.javabot.other;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
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

    /**
     * A map of database names and collection names.
     *
     * <p>The key is the database name. The value is
     * an array of all collections that should be in
     * the corresponding database.</p>
     */
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
    }

    public void deleteOpenSubmissions(Guild guild) {
        logger.info("[{}] Deleting Open Submissions", guild.getName());

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

    public Document userDoc(User user) {
        return new Document("tag", user.getAsTag())
                .append("discord_id", user.getId())
                .append("qotwpoints", 0);
    }

    public void setMemberEntry(String memberID, String path, Object newValue) {
        Document setData = new Document(path, newValue);
        Document update = new Document("$set", setData);

        Document query = new Document("discord_id", memberID);
        mongoClient.getDatabase("userdata")
                .getCollection("users")
                .updateOne(query, update);
    }

    private <T> T getMember(Member member, String path, T defaultValue) {
        try {
            Document doc = mongoClient
                    .getDatabase("userdata")
                    .getCollection("users")
                    .find(eq("discord_id", member.getId()))
                    .first();
            String[] splittedPath = path.split("\\.");
            int pathLen = splittedPath.length - 1;

            for (int i = 0; i < pathLen; ++i) {
                doc = doc.get(splittedPath[i], Document.class);
            }
            return doc.get(splittedPath[pathLen], defaultValue);
        } catch (Exception e) { e.printStackTrace(); return defaultValue; }
    }

    public String getMemberString(Member member, String path) {
        return getMember(member, path, "None");
    }

    public int getMemberInt(Member member, String path) {
        return getMember(member, path, 0);
    }

    public boolean isMessageOnStarboard(String gID, String cID, String mID) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);

        Document first = mongoClient
                .getDatabase("other")
                .getCollection("starboard_messages")
                .find(criteria)
                .first();

        return first != null;
    }

    public void createStarboardDoc(String gID, String cID, String mID, String eMID) {
        Document doc = new Document("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID)
                .append("starboard_embed", eMID);

        mongoClient.getDatabase("other")
                .getCollection("starboard_messages")
                .insertOne(doc);
    }

    public String getStarboardChannelString(String gID, String cID, String mID, String value) {
        BasicDBObject criteria = new BasicDBObject("guild_id", gID)
                .append("channel_id", cID)
                .append("message_id", mID);
        Document first = mongoClient.getDatabase("other")
                .getCollection("starboard_messages")
                .find(criteria)
                .first();
        return first == null ? null : first.getString(value);
    }
}
