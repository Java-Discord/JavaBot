package com.javadiscord.javabot.service.serverlock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.Constants;
import com.javadiscord.javabot.utils.Misc;
import com.javadiscord.javabot.utils.TimeUtils;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.bson.Document;

import java.time.Instant;
import java.time.OffsetDateTime;

import static com.javadiscord.javabot.service.Startup.mongoClient;

/**
 * Server lock functionality that automatically locks the server if a raid is detected.
 */
@Slf4j
public class ServerLock {

    /**
     * Main logic of the server lock system. Decides if the newly joined member should increment the server lock count or not.
     * @param user The user that joined.
     */
    public void checkLock(GuildMemberJoinEvent event, User user) {
        if (isNewAccount(event, user) && !isInPotentialBotList(event.getGuild(), user)) {
            addToPotentialBotList(event.getGuild(), user);
        } else if (!isInPotentialBotList(event.getGuild(), user)) {
                deletePotentialBotList(event.getGuild());
            }
        if (getLockCount(event.getGuild())
                >= Bot.config.get(event.getGuild()).getServerLock().getLockThreshold()) {
            lockServer(event);
        }
    }

    /**
     * Locks the server and kicks all users that are on the "Potential Bot List".
     */
    public void lockServer(GuildMemberJoinEvent event) {
        var docs = mongoClient
                .getDatabase("userdata")
                .getCollection("potential_bot_list")
                .find(new Document("guildId", event.getGuild().getId()));

        for (Document document : docs) {
            JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String id = root.get("userId").getAsString();

            User user = event.getGuild().getMemberById(id).getUser();
            user.openPrivateChannel().queue(c -> {
                c.sendMessage("https://discord.gg/java")
                        .setEmbeds(ServerLock.lockEmbed(event.getGuild())).queue();
                try {
                    event.getGuild().getMemberById(id).kick().queue();
                } catch (Exception e) {
                    Misc.sendToLog(event.getGuild(), String.format("Could not kick member %s%n> `%s`",
                            event.getUser().getAsTag(), e.getMessage()));
                }
            });
        }

        try {
            Bot.config.get(event.getGuild()).set("serverLock.locked", "true");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Couldn't modify lock property");
        }

        deletePotentialBotList(event.getGuild());
        Misc.sendToLog(event.getGuild(), Bot.config.get(event.getGuild()).getServerLock().getLockMessageTemplate());
    }

    /**
     * Returns the current lock status.
     */
    public boolean lockStatus (Guild guild) {
        return Bot.config.get(guild).getServerLock().isLocked();
    }

    /**
     * Checks if the account is older than the set threshold.
     * @param user The user that is checked
     */
    public boolean isNewAccount (GuildMemberJoinEvent event, User user) {
        return user.getTimeCreated().isAfter(OffsetDateTime.now().minusYears(
                Bot.config.get(event.getGuild()).getServerLock().getMinimumAccountAgeInDays()
        )) &&
                !Bot.config.get(event.getGuild()).getServerLock().isLocked();
    }

    /**
     * Checks if a user is already in the Potential Bot List.
     * @param guild The current guild.
     * @param user The user that is checked.
     */
    public boolean isInPotentialBotList(Guild guild, User user) {
        return mongoClient
                .getDatabase("userdata")
                .getCollection("potential_bot_list")
                .find(
                        new BasicDBObject("guildId", guild.getId())
                                .append("userId", user.getId())
                ).first() != null;
    }

    /**
     * Adds a user to the Potential Bot List and sends an embed to the log
     * @param guild The current guild.
     * @param user The user that is being added.
     */
    public void addToPotentialBotList(Guild guild, User user) {
        mongoClient.getDatabase("userdata")
                .getCollection("potential_bot_list")
                .insertOne(
                        new Document("guildId", guild.getId())
                                .append("userId", user.getId())
                );

        String timeCreated = user.getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);
        String createDiff = " (" + new TimeUtils().formatDurationToNow(user.getTimeCreated()) + " ago)";

        var eb = new EmbedBuilder()
                .setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
                .setAuthor(user.getAsTag() + " | Potential Bot! (" + getLockCount(guild)  + "/5)", null, user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField("Account created on", "```" + timeCreated + createDiff + "```", false)
                .setFooter("ID: " + user.getId())
                .setTimestamp(Instant.now())
                .build();

        Bot.config.get(guild).getModeration().getLogChannel()
                .sendMessageEmbeds(eb)
                .setActionRow(
                        Button.danger("utils:ban:" + user.getId(), "Ban"),
                        Button.danger("utils:kick:" + user.getId(), "Kick")
                ).queue();
    }

    /**
     * Deletes all Potential Bot List entries for the given guild.
     * @param guild The current guild.
     */
    public void deletePotentialBotList(Guild guild) {
        mongoClient.getDatabase("userdata")
                .getCollection("potential_bot_list")
                .deleteMany(new BasicDBObject("guildId", guild.getId()));
    }

    public int getLockCount(Guild guild) {
        return (int) mongoClient.getDatabase("userdata")
                .getCollection("potential_bot_list")
                .countDocuments(new Document("guildId", guild.getId()));
    }

    /**
     * The embed that is sent when a user tries to join while the server is locked.
     * @param guild The current guild.
     */
    public static MessageEmbed lockEmbed(Guild guild) {
        return new EmbedBuilder()
        .setAuthor(guild.getName() + " | Server locked \uD83D\uDD12", Constants.WEBSITE_LINK, guild.getIconUrl())
        .setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
        .setDescription(String.format("""
        Unfortunately, this server is currently locked. Please try to join again later.
        Contact the server owner, %s, for more info.""", guild.getOwner().getAsMention())
        ).build();
    }
}
