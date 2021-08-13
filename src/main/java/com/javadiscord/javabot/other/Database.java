package com.javadiscord.javabot.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private Database() {
        throw new UnsupportedOperationException("No instances");
    }

    public static void deleteOpenSubmissions(Guild guild) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("open_submissions");

        for (var document : collection.find(eq("guild_id", guild.getId()))) {

            JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String messageId = root.get("message_id").getAsString();
            String userId = root.get("user_id").getAsString();

            User user = preferredGuild.retrieveMemberById(userId).complete().getUser();
            Message msg = user.openPrivateChannel().complete().retrieveMessageById(messageId).complete();

            msg.editMessageEmbeds(msg.getEmbeds().get(0))
                    .setActionRows(ActionRow.of(
                            Button.danger("dm-submission:canceled:" + user.getId(), "Process canceled").asDisabled()))
                    .queue();

            collection.deleteOne(document);
        }
    }

    public static Document userDoc(Member member) {
        return userDoc(member.getUser());
    }

    public static Document userDoc(User user) {
        return new Document("tag", user.getAsTag())
                .append("discord_id", user.getId())
                .append("qotwpoints", 0);
    }

    public static Document guildDoc(String guildName, String guildID) {
        Document av = new Document("avX", 75)
                .append("avY", 100)
                .append("avH", 400)
                .append("avW", 400);

        Document wi = new Document("avatar", av)
                .append("imgW", 1920)
                .append("imgH", 600)
                .append("overlayURL", "https://cdn.discordapp.com/attachments/744899463591624815/827303132098461696/WelcomeOverlay_NoShadow.png")
                .append("bgURL", "https://cdn.discordapp.com/attachments/744899463591624815/840928322661122048/WelcomeBG.png")
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

        Document roles = new Document("mute_rid", "None")
                .append("staff_rid", "None")
                .append("jam_admin_rid", "None")
                .append("jam_ping_rid", "None");

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

        Document doc = new Document("name", guildName)
                .append("guild_id", guildID)
                .append("welcome_system", ws)
                .append("channels", channels)
                .append("roles", roles)
                .append("other", other);

        return doc;
    }

    public static boolean guildDocExists(Guild guild) {
        if (guild == null) return false;

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        if (collection.find(eq("guild_id", guild.getId())).first() == null) return false;

        return true;
    }

    public static void insertGuildDoc(Guild guild) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        collection.insertOne(guildDoc(guild.getName(), guild.getId()));

        logger.warn("Added Database entry for Guild \"" + guild.getName() + "\" (" + guild.getId() + ")");
    }

    public static void queryMember(String memberID, String varName, String newValue) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        Document query = new Document().append("discord_id", memberID);
        collection.updateOne(query, new Document("$set", new Document(varName, newValue)));
    }

    public static void queryMember(String memberID, String varName, int newValue) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        Document query = new Document().append("discord_id", memberID);
        collection.updateOne(query, new Document("$set", new Document(varName, newValue)));
    }

    public static String getMemberString(User user, String varName) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        try {
            String doc = collection.find(eq("discord_id", user.getId())).first().toJson();
            return JsonParser.parseString(doc)
                    .getAsJsonObject()
                    .get(varName)
                    .getAsString();
        } catch (NullPointerException e) {
            collection.insertOne(userDoc(user));
            return "0";
        }
    }

    public static int getMemberInt(Member member, String varName) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        try {
            String doc = collection.find(eq("discord_id", member.getUser().getId())).first().toJson();
            return JsonParser.parseString(doc)
                    .getAsJsonObject()
                    .get(varName)
                    .getAsInt();
        } catch (NullPointerException e) {
            collection.insertOne(userDoc(member));
            return 0;
        }
    }

    public static void queryConfig(String guildID, String path, String newValue) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        Document query = new Document();
        query.append("guild_id", guildID);

        collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject(path, newValue)));
    }

    public static void queryConfig(String guildID, String path, int newValue) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        Document query = new Document();
        query.append("guild_id", guildID);

        collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject(path, newValue)));
    }

    public static void queryConfig(String guildID, String path, boolean newValue) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        Document query = new Document();
        query.append("guild_id", guildID);

        collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject(path, newValue)));
    }

    public static String getConfigString(Guild guild, String path) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guild.getId())).first().toJson();
            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();

            String[] splittedPath = path.split("\\.");
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();

            String value = root.get(splittedPath[splittedPath.length - 1]).getAsString();
            return value == null ? "None" : value;
        } catch (Exception e) {
            if (!guildDocExists(guild)) insertGuildDoc(guild);
            return "None";
        }
    }

    public static int getConfigInt(Guild guild, String path) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guild.getId())).first().toJson();
            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();

            String[] splittedPath = path.split("\\.");
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();

            return root.get(splittedPath[splittedPath.length - 1]).getAsInt();
        } catch (Exception e) {
            if (!guildDocExists(guild)) insertGuildDoc(guild);
            return 0;
        }
    }

    public static boolean getConfigBoolean(Guild guild, String path) {
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guild.getId())).first().toJson();
            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();

            String[] splittedPath = path.split("\\.");
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();

            return root.get(splittedPath[splittedPath.length - 1]).getAsBoolean();
        } catch (Exception e) {
            if (!guildDocExists(guild)) insertGuildDoc(guild);
            return false;
        }
    }

    public static TextChannel getConfigChannel(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getTextChannelById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Role getConfigRole(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getRoleById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getConfigChannelAsMention(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getTextChannelById(id).getAsMention();
        } catch (NumberFormatException e) {
            return "None";
        }
    }

    public static String getConfigRoleAsMention(Guild guild, String path) {
        String id = getConfigString(guild, path);
        try {
            return guild.getRoleById(id).getAsMention();
        } catch (NumberFormatException e) {
            return "None";
        }
    }
}
