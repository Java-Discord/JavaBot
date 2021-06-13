package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.TimeUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.bson.Document;

import java.awt.*;
import java.util.List;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Profile {

    public static void execute(SlashCommandEvent event, Member member) {

        String highestRole;
        try {
            highestRole = member.getRoles().get(0).getName();
        } catch (IndexOutOfBoundsException e) {
            highestRole = "everyone";
        }

        String timeJoined = member.getTimeJoined().format(TimeUtils.STANDARD_FORMATTER);
        String timeCreated = member.getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);

        Color color = null;
        String colorText;
        if (member.getColor() == null) {
            color = new Color(0x2F3136);
            colorText = "None";

        } else {
            color = member.getColor();
            colorText = "#" + Integer.toHexString(member.getColorRaw());
        }

        String status = member.getOnlineStatus().toString().substring(0, 1).toUpperCase() + member.getOnlineStatus().toString().substring(1).toLowerCase().replace("_", " ");
        List<Activity> activities = member.getActivities();

        String customStatus, gameActivity;

        try {
            if (activities.get(0).getType().toString().equalsIgnoreCase("CUSTOM_STATUS")) {
                customStatus = activities.get(0).getName();
            } else {
                customStatus = "None";
            }
        } catch (IndexOutOfBoundsException e) {
            customStatus = "None";
        }

        try {
            if (customStatus.equals("None")) {
                gameActivity = activities.get(0).getName();

            } else {
                gameActivity = activities.get(1).getName();
            }
        } catch (IndexOutOfBoundsException e) {
            gameActivity = "None";
        }

        try {

            List<Emote> emoteOnline = event.getGuild().getEmotesByName("sOnline", false);
            List<Emote> emoteIdle = event.getGuild().getEmotesByName("sIdle", false);
            List<Emote> emoteDND = event.getGuild().getEmotesByName("sDND", false);
            List<Emote> emoteOffline = event.getGuild().getEmotesByName("sOffline", false);
            List<Emote> emoteBrilliance = event.getGuild().getEmotesByName("badgeBrilliance", false);
            List<Emote> emoteBalance = event.getGuild().getEmotesByName("badgeBalance", false);
            List<Emote> emoteBravery = event.getGuild().getEmotesByName("badgeBravery", false);
            List<Emote> emoteNitro = event.getGuild().getEmotesByName("badgeNitro", false);
            List<Emote> emoteESupporter = event.getGuild().getEmotesByName("badgeESupporter", false);
            List<Emote> emotePartner = event.getGuild().getEmotesByName("badgePartner", false);
            List<Emote> emoteDev = event.getGuild().getEmotesByName("badgeDev", false);
            List<Emote> emoteStaff = event.getGuild().getEmotesByName("badgeStaff", false);
            List<Emote> emoteServerBoost = event.getGuild().getEmotesByName("badgeServerBoost", false);
            List<Emote> emoteBugHunter = event.getGuild().getEmotesByName("badgeBugHunter", false);
            List<Emote> emoteBot = event.getGuild().getEmotesByName("badgeBot", false);

            String statusEmote = status
                    .replace("Online", emoteOnline.get(0).getAsMention())
                    .replace("Idle", emoteIdle.get(0).getAsMention())
                    .replace("Do not disturb", emoteDND.get(0).getAsMention())
                    .replace("Offline", emoteOffline.get(0).getAsMention());

            String badges = member.getUser().getFlags().toString()
                    .substring(1, member.getUser().getFlags().toString().length() - 1)
                    .replace(",", "")
                    .replace("PARTNER", emotePartner.get(0).getAsMention())
                    .replace("HYPESQUAD_BRAVERY", emoteBravery.get(0).getAsMention())
                    .replace("HYPESQUAD_BRILLIANCE", emoteBrilliance.get(0).getAsMention())
                    .replace("HYPESQUAD_BALANCE", emoteBalance.get(0).getAsMention())
                    .replace("VERIFIED_DEVELOPER", emoteDev.get(0).getAsMention())
                    .replace("EARLY_SUPPORTER", emoteESupporter.get(0).getAsMention())
                    .replace("SYSTEM", emoteStaff.get(0).getAsMention())
                    .replace("BUG_HUNTER_LEVEL_1", emoteBugHunter.get(0).getAsMention())
                    .replace("BUG_HUNTER_LEVEL_2", emoteBugHunter.get(0).getAsMention())
                    .replace("VERIFIED_BOT", "");

            String boostBadge, botBadge;

            if (!(member.getTimeBoosted() == null)) {
                boostBadge = emoteServerBoost.get(0).getAsMention();
            } else {
                boostBadge = "";
            }

            String name = member.getEffectiveName()
                    .replace("```", "");

            if (member.getUser().isBot()) {
                botBadge = emoteBot.get(0).getAsMention();
            } else {
                botBadge = "";
            }

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> collection = database.getCollection("users");

            int qotwCount = Database.getMemberInt(collection, member, "qotwpoints");
            int warnCount = Database.getMemberInt(collection, member, "warns");

            TimeUtils tu = new TimeUtils();
            String joinDiff = " (" + tu.formatDurationToNow(member.getTimeJoined()) + ")";
            String createDiff = " (" + tu.formatDurationToNow(member.getTimeCreated()) + ")";

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(statusEmote + " " + member.getUser().getAsTag() + " " + botBadge + boostBadge + badges)
                    .setColor(color)
                    .setThumbnail(member.getUser().getEffectiveAvatarUrl() + "?size=4096")
                    .addField("Name", "```" + name + " ```", true)
                    .addField("QOTW", "```" + qotwCount + " (#" + Leaderboard.rank(member.getId()) + ")```", true)
                    .addField("Warnings", "```" + warnCount + "```", true)
                    .addField("ID", "```" + member.getId() + "```", false)
                    .addField("Role Amount", "```" + member.getRoles().size() + " Roles```", true)
                    .addField("Role Color", "```" + colorText + "```", true)
                    .addField("Highest Role", "```@" + highestRole + "```", true)
                    .addField("Custom Status", "```" + customStatus + "```", true)
                    .addField("Game Activity", "```" + gameActivity + "```", true)
                    .addField("Server joined on", "```" + timeJoined + joinDiff + "```", false)
                    .addField("Account created on", "```" + timeCreated + createDiff + "```", false);
            event.replyEmbeds(eb.build()).queue();

        } catch (IndexOutOfBoundsException e) {
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event)).setEphemeral(true).queue();
        }
    }
}
