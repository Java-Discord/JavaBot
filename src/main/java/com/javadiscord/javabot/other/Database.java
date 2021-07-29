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

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

public class Database {

    public void deleteOpenSubmissions (Guild guild) {

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

    public Document userDoc (Member member) {

        return userDoc(member.getUser());
    }

    public Document userDoc(User user) {

        Document doc = new Document("tag", user.getAsTag())
                .append("discord_id", user.getId())
                .append("qotwpoints", 0);

        return doc;
    }

    public Document guildDoc (String guildName, String guildID) {

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

    public void queryMember(String memberID, String varName, String newValue) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        Document Query = new Document();
        Query.append("discord_id", memberID);

        Document SetData = new Document();
        SetData.append(varName, newValue);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(Query, update);
    }

    public void queryMember(String memberID, String varName, int newValue) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        Document Query = new Document();
        Query.append("discord_id", memberID);

        Document SetData = new Document();
        SetData.append(varName, newValue);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(Query, update);
    }

    public String getMemberString(User user, String varName) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        try {
            String doc = collection.find(eq("discord_id", user.getId())).first().toJson();

            JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
            String var = Root.get(varName).getAsString();
            return var;

        } catch (NullPointerException e) {

            collection.insertOne(userDoc(user));
            return "0";
        }
    }

    public int getMemberInt(Member member, String varName) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        try {
            String doc = collection.find(eq("discord_id", member.getUser().getId())).first().toJson();

            JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
            int var = Root.get(varName).getAsInt();
            return var;

        } catch (NullPointerException e) {

            collection.insertOne(userDoc(member));
            return 0;
        }
    }

    public void queryConfig(String guildID, String path, String newValue) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        Document query = new Document();
        query.append("guild_id", guildID);

        collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject(path, newValue)));
    }

    public void queryConfig(String guildID, String path, int newValue) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        Document query = new Document();
        query.append("guild_id", guildID);

        collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject(path, newValue)));
    }

    public void queryConfig(String guildID, String path, boolean newValue) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        Document query = new Document();
        query.append("guild_id", guildID);

        collection.updateOne(query, new BasicDBObject("$set", new BasicDBObject(path, newValue)));
    }

    public String getConfigString(Guild guild, String path) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guild.getId())).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            String var = root.get(splittedPath[splittedPath.length - 1]).getAsString();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guild.getName(), guild.getId()));
            return "None";
        }
    }

    public int getConfigInt(Guild guild, String path) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guild.getId())).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            int var = root.get(splittedPath[splittedPath.length - 1]).getAsInt();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guild.getName(), guild.getId()));
            return 0;
        }
    }

    public boolean getConfigBoolean (Guild guild, String path) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guild.getId())).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            boolean var = root.get(splittedPath[splittedPath.length - 1]).getAsBoolean();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guild.getName(), guild.getId()));
            return false;
        }
    }

    public TextChannel getConfigChannel (Guild guild, String path) {

        String id = getConfigString(guild, path);
        return guild.getTextChannelById(id);
    }

    public Role getConfigRole (Guild guild, String path) {

        String id = getConfigString(guild, path);
        return guild.getRoleById(id);
    }

    public String getConfigChannelAsMention (Guild guild, String path) {

            String mention;
            String id = getConfigString(guild, path);

            try { mention = guild.getTextChannelById(id).getAsMention(); }
            catch (NumberFormatException e) { mention = "None"; }

        return mention;
    }

    public String getConfigRoleAsMention (Guild guild, String path) {

        String mention;
        String id = getConfigString(guild, path);

        try { mention = guild.getRoleById(id).getAsMention(); }
        catch (NumberFormatException e) { mention = "None"; }

        return mention;
    }

}
