package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.TimeUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bson.Document;

import java.awt.*;
import java.util.List;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Profile implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        OptionMapping profileOption = event.getOption("user");
        Member member = profileOption == null ? event.getMember() : profileOption.getAsMember();

        String highestRole;
        try {
            highestRole = member.getRoles().get(0).getName();
        } catch (IndexOutOfBoundsException e) {
            highestRole = "everyone";
        }

        String timeJoined = member.getTimeJoined().format(TimeUtils.STANDARD_FORMATTER);
        String timeCreated = member.getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);

        Color color;
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

            String statusEmote = status
                .replace("Online", Constants.ONLINE)
                .replace("Idle", Constants.IDLE)
                .replace("Do not disturb", Constants.DND)
                .replace("Offline", Constants.OFFLINE);

            String badges = member.getUser().getFlags().toString()
                .substring(1, member.getUser().getFlags().toString().length() - 1)
                .replace(",", "")
                .replace("PARTNER", Constants.PARTNER)
                .replace("HYPESQUAD_BRAVERY", Constants.BRAVERY)
                .replace("HYPESQUAD_BRILLIANCE", Constants.BRILLIANCE)
                .replace("HYPESQUAD_BALANCE", Constants.BALANCE)
                .replace("VERIFIED_DEVELOPER", Constants.DEV)
                .replace("EARLY_SUPPORTER", Constants.EARLY_SUPPORTER)
                .replace("SYSTEM", Constants.STAFF)
                .replace("BUG_HUNTER_LEVEL_1", Constants.BUG_HUNTER)
                .replace("BUG_HUNTER_LEVEL_2", Constants.BUG_HUNTER)
                .replace("VERIFIED_BOT", "");

            String boostBadge, botBadge;

            if (!(member.getTimeBoosted() == null)) {
                boostBadge = Constants.SERVER_BOOST;
            } else {
                boostBadge = "";
            }

            String name = member.getEffectiveName()
                .replace("```", "");

            if (member.getUser().isBot()) {
                botBadge = Constants.BOT;
            } else {
                botBadge = "";
            }

            MongoDatabase database = mongoClient.getDatabase("userdata");
            MongoCollection<Document> warns = database.getCollection("warns");

            int qotwCount = Database.getMemberInt(member, "qotwpoints");
            int warnCount = (int) warns.count(eq("user_id", member.getId()));

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
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).setEphemeral(true).queue();
        }
    }
}
