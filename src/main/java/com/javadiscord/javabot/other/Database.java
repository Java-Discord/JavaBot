package com.javadiscord.javabot.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Database {

    public static Document userDoc (Member member) {

        return userDoc(member.getUser());
    }

    public static Document userDoc (User user) {

        Document doc = new Document("tag", user.getAsTag())
                .append("discord_id", user.getId())
                .append("qotwpoints", 0)
                .append("qotw-guild", "");

        return doc;
    }

    public static Document guildDoc (String guildName, String guildID) {

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
                .append("submission_cid", "None");

        Document roles = new Document("mute_rid", "None");

        Document stats = new Document("stats_cid", "None")
                .append("stats_text", "None");

        Document qotw = new Document("dm-qotw", false);

        Document lock = new Document("lock_status", false)
                .append("lock_count", 0);

        Document sb = new Document("starboard_cid", "None")
                .append("starboard_emote", "⭐");

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

    public static void queryMemberString(String memberID, String varName, String newValue) {

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

    public static void queryMemberInt(String memberID, String varName, int newValue) {

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

    public static String getMemberString(Member member, String varName) {

        return getMemberString(member.getUser(), varName);
    }

    public static String getMemberString(User user, String varName) {

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

    public static int getMemberInt(MongoCollection<Document> collection, Member member, String varName) {

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

    public static String getConfigString(Object event, String path) {

        String guildName = null, guildID = null;

        if (event instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent e = (SlashCommandEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent e = (GuildMemberRemoveEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent) {
            net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent e = (GuildMessageReactionAddEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guildID)).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            String var = root.get(splittedPath[splittedPath.length - 1]).getAsString();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guildName, guildID));
            return "None";
        }
    }

    public static String getConfigString(String guildName, String guildID, String path) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guildID)).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            String var = root.get(splittedPath[splittedPath.length - 1]).getAsString();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guildName, guildID));
            return "None";
        }
    }

    public static int getConfigInt(Object event, String path) {

        String guildName = null, guildID = null;

        if (event instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent e = (SlashCommandEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guildID)).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            int var = root.get(splittedPath[splittedPath.length - 1]).getAsInt();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guildName, guildID));
            return 0;
        }
    }

    public static int getConfigInt(String guildName, String guildID, String path) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guildID)).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            int var = root.get(splittedPath[splittedPath.length - 1]).getAsInt();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guildName, guildID));
            return 0;
        }
    }

    public static boolean getConfigBoolean(Object event, String path) {

        String guildName = null, guildID = null;

        if (event instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent e = (SlashCommandEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        if (event instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;

            guildID = e.getGuild().getId();
            guildName = e.getGuild().getName();
        }

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        try {
            String doc = collection.find(eq("guild_id", guildID)).first().toJson();
            String[] splittedPath = path.split("\\.");

            JsonObject root = JsonParser.parseString(doc).getAsJsonObject();
            for (int i = 0; i < splittedPath.length - 1; i++) root = root.get(splittedPath[i]).getAsJsonObject();
            boolean var = root.get(splittedPath[splittedPath.length - 1]).getAsBoolean();

            return var;

        } catch (NullPointerException e) {

            e.printStackTrace();
            collection.insertOne(guildDoc(guildName, guildID));
            return false;
        }
    }

    public static TextChannel getConfigChannel(Object event, String varName) {

        TextChannel tc = null;

        if (event instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent e = (SlashCommandEvent) event;

            String id = getConfigString(event, varName);
            tc = e.getGuild().getTextChannelById(id);
        }

        if (event instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;

            String id = getConfigString(event, varName);
            tc = e.getGuild().getTextChannelById(id);
        }

        if (event instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;

            String id = getConfigString(event, varName);
            tc = e.getGuild().getTextChannelById(id);
        }

        return tc;
    }

    public static Role getConfigRole(Object event, String varName) {

        Role role = null;

        if (event instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent e = (SlashCommandEvent) event;

            String id = getConfigString(event, varName);
            role = e.getGuild().getRoleById(id);
        }

        if (event instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;

            String id = getConfigString(event, varName);
            role = e.getGuild().getRoleById(id);
        }

        if (event instanceof net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent) {
            net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;

            String id = getConfigString(event, varName);
            role = e.getGuild().getRoleById(id);
        }

        return role;
    }

    public static String getConfigChannelAsMention(SlashCommandEvent event, String varName) {

            String mention;
            String id = getConfigString(event, varName);

            try { mention = event.getGuild().getTextChannelById(id).getAsMention(); }
            catch (NumberFormatException e) { mention = "None"; }

        return mention;
    }

    public static String getConfigRoleAsMention(SlashCommandEvent event, String varName) {

        String mention;
        String id = getConfigString(event, varName);

        try { mention = event.getGuild().getRoleById(id).getAsMention(); }
        catch (NumberFormatException e) { mention = "None"; }

        return mention;
    }

}
