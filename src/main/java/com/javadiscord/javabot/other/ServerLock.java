package com.javadiscord.javabot.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class ServerLock {

    public static void checkLock(GuildMemberJoinEvent event, User user) {

        if (isNewAccount(event, user) && !isInPBL(user)) {

            incrementLock(event, user);
            addToPBL(user);
        }

        else {

            if (!isInPBL(user)) {

                Database.queryConfigInt(event.getGuild().getId(), "lockcount", 0);
                deletePBL();
            }
        }

        if (Database.getConfigInt(event, "lockcount") >= 5) {

            lockServer(event);
        }
    }


    public static void incrementLock(GuildMemberJoinEvent event, User user) {

        int lockCount = Database.getConfigInt(event, "lockcount");
        lockCount = lockCount + 1;
        Database.queryConfigInt(event.getGuild().getId(), "lockcount", lockCount);

        String timeCreated = user.getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);
        String createDiff = " (" + TimeUtils.formatDurationToNow(user.getTimeCreated()) + " ago)";

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setAuthor(user.getAsTag() + " | Potential Bot! (" + lockCount  + "/5)")
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField("Account created on", "```" + timeCreated + createDiff + "```", false)
                .setFooter("ID: " + user.getId())
                .setTimestamp(new Date().toInstant());

        Misc.sendToLog(event, eb.build());

    }

    public static void lockServer(GuildMemberJoinEvent event) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");

        MongoCursor<Document> doc = collection.find().iterator();

        while (doc.hasNext()) {
            JsonObject Root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
            String discordID = Root.get("discord_id").getAsString();

            User user = event.getGuild().getMemberById(discordID).getUser();
            user.openPrivateChannel().complete().sendMessage(lockEmbed(event.getGuild())).queue();
            event.getGuild().getMemberById(discordID).kick().complete();
        }


        Database.queryConfigString(event.getGuild().getId(), "lock", "true");
        Database.queryConfigInt(event.getGuild().getId(), "lockcount", 0);
        deletePBL();

        String logCid = Database.getConfigString(event, "log_cid");
        event.getGuild().getTextChannelById(logCid).sendMessage("**SERVER LOCKED!** @here").queue();

    }

    public static boolean lockStatus (GuildMemberJoinEvent event) {

        return Boolean.parseBoolean(Database.getConfigString(event, "lock"));
    }

    public static boolean isNewAccount (GuildMemberJoinEvent event, User user) {

        return user.getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) && Database.getConfigString(event, "lock").equalsIgnoreCase("false");
    }

    public static boolean isInPBL (User user) {

        boolean isInPBL = true;

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");

        try {
            String doc = collection.find(eq("discord_id", user.getId())).first().toJson();

        } catch (NullPointerException e) {
            isInPBL = false;
        }

        return isInPBL;
    }

    public static void addToPBL (User user) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");

        Document doc = new Document("tag", user.getAsTag())
                .append("discord_id", user.getId());
        collection.insertOne(doc);
    }

    public static void deletePBL () {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");

        MongoCursor<Document> doc = collection.find().iterator();

        while (doc.hasNext()) {
            collection.deleteOne(doc.next());
        }
    }

    public static MessageEmbed lockEmbed (Guild guild) {

        EmbedBuilder eb = new EmbedBuilder()
        .setAuthor(guild.getName() + " | Server locked \uD83D\uDD12", Constants.WEBSITE, guild.getIconUrl())
        .setColor(Constants.GRAY)
        .setDescription("Unfortunately, this server is currently locked. Please try to join again later.\nContact ``Dynxsty#7666`` or ``Moon™#3424`` for more info.");

        return eb.build();
    }
}
