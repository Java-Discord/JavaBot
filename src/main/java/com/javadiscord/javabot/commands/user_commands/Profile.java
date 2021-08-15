package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.moderation.Warns;
import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;

public class Profile implements SlashCommandHandler {

    String getBadges (Member member) {

        String badges = "";

        if (member.getUser().isBot()) badges += Constants.BOT;
        if (!(member.getTimeBoosted() == null)) badges += Constants.SERVER_BOOST;

        badges += member.getUser().getFlags().toString()
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

        return badges;
    }

    String getOnlineStatus (Member member) {

        String statusEmote = member.getOnlineStatus().toString()
                .replace("ONLINE", Constants.ONLINE)
                .replace("IDLE", Constants.IDLE)
                .replace("DO_NOT_DISTURB", Constants.DND)
                .replace("OFFLINE", Constants.OFFLINE);

        return statusEmote;
    }

    Color getColor (Member member) {

        Color color;
        if (member.getColor() == null) color = Constants.GRAY;
        else color = member.getColor();

        return color;
    }

    String getColorRoleAsMention (Member member) {

        String highestRole;
        try {
            highestRole = member.getRoles().get(0).getAsMention();
        } catch (IndexOutOfBoundsException e) {
            highestRole = "None";
        }

        return highestRole;
    }

    String getColorAsHex (Color color) {
        return "#" + Integer.toHexString(color.getRGB()).toUpperCase();
    }

    Activity getCustomActivity (Member member) {

        Activity activity = null;

        for (var act : member.getActivities()) {

            if (act.getType().name().equals("CUSTOM_STATUS")) {
                activity = act;
                break;
            }
        }

        return activity;
    }

    Activity getGameActivity (Member member) {

        Activity activity = null;

        for (var act : member.getActivities()) {

            if (act.getType().name().equals("CUSTOM_STATUS")) continue;
            else activity = act; break;
        }

        return activity;
    }

    String getGameActivityType (Activity activity) {

        return activity.getType().name().toLowerCase()
                .replace("listening", "Listening to")
                .replace("default", "Playing");
    }

    String getGameActivityDetails (Activity activity) {

        String details = "";

        if (activity.getName().equals("Spotify")) {

            RichPresence rp = activity.asRichPresence();
            String spotifyURL = "https://open.spotify.com/track/" + rp.getSyncId();

            details = "[`\"" + rp.getDetails() + "\"";
            if (!(rp.getState() == null)) details +=  " by " + rp.getState();
            details += "`](" + spotifyURL + ") " + Constants.SPOTIFY;

        } else details = "`" + activity.getName() + "`";

        return details;
    }

    String getServerJoinedDate (Member member, TimeUtils tu) {

        long timeJoined = member.getTimeJoined().toInstant().getEpochSecond();
        String joinDiff = tu.formatDurationToNow(member.getTimeJoined());

        return "<t:" + timeJoined + ":F>" + " (" + joinDiff + " ago)";
    }

    String getAccountCreatedDate (Member member, TimeUtils tu) {

        long timeCreated = member.getTimeCreated().toInstant().getEpochSecond();
        String createDiff = tu.formatDurationToNow(member.getTimeCreated());

        return "<t:" + timeCreated + ":F>" + " (" + createDiff + " ago)";
    }

    String getDescription (Member member) {

        String desc = "";


        if (!(getCustomActivity(member) == null)) desc += "\n\"" + getCustomActivity(member).getName() + "\"";
        if (!(getGameActivity(member) == null)) desc += "\n• " + getGameActivityType(getGameActivity(member)) + " " + getGameActivityDetails(getGameActivity(member));

        desc +=
                "\n\n⌞ Warnings: `" + new Warns().warnCount(member) + "`" +
                "\n⌞ QOTW-Points: `" + new Database().getMemberInt(member, "qotwpoints") + " (#" + new Leaderboard().getQOTWRank(member.getGuild(), member.getId()) + ")`";

        return desc;
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        OptionMapping profileOption = event.getOption("user");
        Member member = profileOption == null ? event.getMember() : profileOption.getAsMember();

        TimeUtils tu = new TimeUtils();

            var e = new EmbedBuilder()
                .setTitle(getOnlineStatus(member) + " " + member.getUser().getAsTag() + " " + getBadges(member))
                .setThumbnail(member.getUser().getEffectiveAvatarUrl() + "?size=4096")

                .setColor(getColor(member))
                .setDescription(getDescription(member))
                .setFooter("ID: " + member.getId());

                if (member.getRoles().size() > 1 ) e.addField("Roles", getColorRoleAsMention(member) + " (+" + (member.getRoles().size() -1) + " other)", true);
                else if (member.getRoles().size() > 0) e.addField("Roles", getColorRoleAsMention(member), true);

                if (member.getRoles().size() > 0) e.addField("Color", "`" + getColorAsHex(getColor(member)) + "`", true);
                    e.addField("Server joined on", getServerJoinedDate(member, tu), false)
                    .addField("Account created on", getAccountCreatedDate(member, tu), true);

            return event.replyEmbeds(e.build());
    }
}
