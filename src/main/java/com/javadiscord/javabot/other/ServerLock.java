package com.javadiscord.javabot.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import org.bson.Document;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class ServerLock {

    public static void checkLock(GuildMemberJoinEvent event, User user) {
        if (isNewAccount(event, user) && !isInPBL(user)) {
            incrementLock(event, user);
            addToPotentialBotList(user);
        } else {
            if (!isInPBL(user)) {
                new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_count", 0);
                deletePotentialBotList();
            }
        }
        if (new Database().getConfigInt(
                event.getGuild(), "other.server_lock.lock_count") >= 5)
            lockServer(event);
        }

    public static void incrementLock(GuildMemberJoinEvent event, User user) {
        int lockCount = new Database().getConfigInt(event.getGuild(), "other.server_lock.lock_count") + 1;
        new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_count", lockCount);
        String timeCreated = user.getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);
        String createDiff = " (" + new TimeUtils().formatDurationToNow(user.getTimeCreated()) + " ago)";

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setAuthor(user.getAsTag() + " | Potential Bot! (" + lockCount  + "/5)", null, user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField("Account created on", "```" + timeCreated + createDiff + "```", false)
                .setFooter("ID: " + user.getId())
                .setTimestamp(new Date().toInstant());
        Misc.sendToLog(event.getGuild(), eb.build());
    }

    public static void lockServer(GuildMemberJoinEvent event) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");
        for (Document document : collection.find()) {
            JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String discordID = root.get("discord_id").getAsString();

            User user = event.getGuild().getMemberById(discordID).getUser();
            user.openPrivateChannel().complete().sendMessageEmbeds(lockEmbed(event.getGuild())).queue();
            event.getGuild().getMemberById(discordID).kick().complete();
        }
        new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_status", true);
        new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_count", 0);
        deletePotentialBotList();

        Misc.sendToLog(event.getGuild(), "**SERVER LOCKED!** @here");
    }

    public static boolean lockStatus (GuildMemberJoinEvent event) {
        return new Database().getConfigBoolean(event.getGuild(), "other.server_lock.lock_status");
    }

    public static boolean isNewAccount (GuildMemberJoinEvent event, User user) {
        return user.getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) &&
                !(new Database().getConfigBoolean(event.getGuild(), "other.server_lock.lock_status"));
    }

    public static boolean isInPBL (User user) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");
        return collection.find(eq("discord_id", user.getId())).first() != null;
    }

    public static void addToPotentialBotList(User user) {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");
        Document doc = new Document("tag", user.getAsTag())
                .append("discord_id", user.getId());
        collection.insertOne(doc);
    }

    public static void deletePotentialBotList() {
        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("potential_bot_list");
        for (Document document : collection.find()) collection.deleteOne(document);
    }

    public static MessageEmbed lockEmbed (Guild guild) {
        return new EmbedBuilder()
        .setAuthor(guild.getName() + " | Server locked \uD83D\uDD12", Constants.WEBSITE_LINK, guild.getIconUrl())
        .setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
        .setDescription("""
        Unfortunately, this server is currently locked. Please try to join again later.
        Contact ``Dynxsty#7666`` or ``Moon™#3424`` for more info."""
        ).build();
    }
}
