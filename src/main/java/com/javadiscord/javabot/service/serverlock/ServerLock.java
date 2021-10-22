package com.javadiscord.javabot.service.serverlock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.Constants;
import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.data.mongodb.Database;
import com.javadiscord.javabot.service.serverlock.subcommands.SetServerLockStatus;
import com.javadiscord.javabot.utils.Misc;
import com.javadiscord.javabot.utils.TimeUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.time.Instant;
import java.time.OffsetDateTime;

import static com.javadiscord.javabot.service.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class ServerLock extends DelegatingCommandHandler {

    public ServerLock() {
        addSubcommand("set", new SetServerLockStatus());
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        try { return super.handle(event);
        } catch (Exception e) { return Responses.error(event, "```" + e.getMessage() + "```"); }
    }

    /**
     * Main logic of the server lock system. Decides if the newly joined member should increment the server lock count or not.
     * @param user The user that joined.
     */
    public static void checkLock(GuildMemberJoinEvent event, User user) {
        if (isNewAccount(event, user) && !isInPotentialBotList(event.getGuild(), user)) {
            incrementLock(event, user);
            addToPotentialBotList(event.getGuild(), user);
        } else {
            if (!isInPotentialBotList(event.getGuild(), user)) {
                new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_count", 0);
                deletePotentialBotList(event.getGuild());
            }
        }
        if (new Database().getConfigInt(
                event.getGuild(), "other.server_lock.lock_count")
                >= Bot.config.get(event.getGuild()).getServerLock().getLockThreshold())
            lockServer(event);
        }

    /**
     * Increments the total lock count for the current server by one and sends an embed to the log channel.
     * @param user The user that joined.
     */
    public static void incrementLock(GuildMemberJoinEvent event, User user) {
        int lockCount = new Database().getConfigInt(event.getGuild(), "other.server_lock.lock_count") + 1;
        new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_count", lockCount);
        String timeCreated = user.getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);
        String createDiff = " (" + new TimeUtils().formatDurationToNow(user.getTimeCreated()) + " ago)";

        var eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setAuthor(user.getAsTag() + " | Potential Bot! (" + lockCount  + "/5)", null, user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField("Account created on", "```" + timeCreated + createDiff + "```", false)
                .setFooter("ID: " + user.getId())
                .setTimestamp(Instant.now())
                .build();

        Bot.config.get(event.getGuild()).getModeration().getLogChannel()
                .sendMessageEmbeds(eb)
                .setActionRow(
                        Button.danger("utils:ban:" + user.getId(), "Ban"),
                        Button.danger("utils:kick:" + user.getId(), "Kick")
                ).queue();
    }

    /**
     * Locks the server and kicks all users that are on the "Potential Bot List".
     */
    public static void lockServer(GuildMemberJoinEvent event) {
        var docs = mongoClient
                .getDatabase("userdata")
                .getCollection("potential_bot_list")
                .find(new Document("guild_id", event.getGuild().getId()));

        for (Document document : docs) {
            JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String id = root.get("discord_id").getAsString();

            User user = event.getGuild().getMemberById(id).getUser();
            user.openPrivateChannel().queue(
                    c -> c.sendMessage("https://discord.gg/java").setEmbeds(lockEmbed(event.getGuild())).queue());
            event.getGuild().getMemberById(id).kick().queue();
        }
        new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_status", true);
        new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_count", 0);
        deletePotentialBotList(event.getGuild());

        Misc.sendToLog(event.getGuild(), "**SERVER LOCKED!** @here");
    }

    /**
     * Returns the current lock status.
     */
    public static boolean lockStatus (GuildMemberJoinEvent event) {
        return new Database().getConfigBoolean(event.getGuild(), "other.server_lock.lock_status");
    }

    /**
     * Checks if the account is older than the set threshold.
     * @param user The user that is checked
     */
    public static boolean isNewAccount (GuildMemberJoinEvent event, User user) {
        return user.getTimeCreated().isAfter(OffsetDateTime.now().minusDays(
                Bot.config.get(event.getGuild()).getServerLock().getAccountAgeThreshold()
        )) &&
                !(new Database().getConfigBoolean(event.getGuild(), "other.server_lock.lock_status"));
    }

    /**
     * Checks if a user is already in the Potential Bot List.
     * @param guild The current guild.
     * @param user The user that is checked.
     */
    public static boolean isInPotentialBotList(Guild guild, User user) {
        return mongoClient
                .getDatabase("userdata")
                .getCollection("potential_bot_list")
                .find(
                        new BasicDBObject("guildId", guild.getId())
                                .append("userId", user.getId())
                ).first() != null;
    }

    /**
     * Adds a user to the Potential Bot List.
     * @param guild The current guild.
     * @param user The user that is being added.
     */
    public static void addToPotentialBotList(Guild guild, User user) {
        mongoClient.getDatabase("userdata")
                .getCollection("potential_bot_list")
                .insertOne(
                        new Document("guildId", guild.getId())
                                .append("userId", user.getId())
                );
    }

    /**
     * Deletes all Potential Bot List entries for the given guild.
     * @param guild The current guild.
     */
    public static void deletePotentialBotList(Guild guild) {
        mongoClient.getDatabase("userdata")
                .getCollection("potential_bot_list")
                .deleteMany(new BasicDBObject("guildId", guild.getId()));
    }

    /**
     * The embed that is sent when a user tries to join while the server is locked.
     * @param guild The current guild.
     */
    public static MessageEmbed lockEmbed(Guild guild) {
        return new EmbedBuilder()
        .setAuthor(guild.getName() + " | Server locked \uD83D\uDD12", Constants.WEBSITE_LINK, guild.getIconUrl())
        .setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
        .setDescription("""
        Unfortunately, this server is currently locked. Please try to join again later.
        Contact ``Dynxsty#7666`` or ``Moon™#3424`` for more info."""
        ).build();
    }
}
