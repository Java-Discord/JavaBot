package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.moderation.Warns;
import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;

public class Profile implements SlashCommandHandler {

    String getBadges (Member member) {
        String badges = "";
        var config = Bot.config.get(member.getGuild()).getEmote();
        if (member.getUser().isBot()) badges += config.getBotBadge();
        if (member.getTimeBoosted() != null) badges += config.getServerBoostBadge();

        badges += member.getUser().getFlags().toString()
                .substring(1, member.getUser().getFlags().toString().length() - 1)
                .replace(",", "")
                .replace("PARTNER", config.getPartnerBadge())
                .replace("HYPESQUAD_BRAVERY", config.getBraveryBadge())
                .replace("HYPESQUAD_BRILLIANCE", config.getBrillianceBadge())
                .replace("HYPESQUAD_BALANCE", config.getBalanceBadge())
                .replace("VERIFIED_DEVELOPER", config.getDevBadge())
                .replace("EARLY_SUPPORTER", config.getEarlySupporterBadge())
                .replace("SYSTEM", config.getStaffBadge())
                .replace("BUG_HUNTER_LEVEL_1", config.getBugHunterBadge())
                .replace("BUG_HUNTER_LEVEL_2", config.getBugHunterBadge())
                .replace("VERIFIED_BOT", "");
        return badges;
    }

    String getOnlineStatus (Member member) {
        var config = Bot.config.get(member.getGuild()).getEmote();
        return member.getOnlineStatus().toString()
                .replace("ONLINE", config.getOnlineEmote())
                .replace("IDLE", config.getIdleEmote())
                .replace("DO_NOT_DISTURB", config.getDndBadge())
                .replace("OFFLINE", config.getOfflineEmote());
    }

    Color getColor (Member member) {
        if (member.getColor() == null) return Color.decode(
                Bot.config.get(member.getGuild()).getSlashCommand().getDefaultColor());
        else return member.getColor();
    }

    String getColorRoleAsMention (Member member) {
        try {
            return member.getRoles().get(0).getAsMention();
        } catch (IndexOutOfBoundsException e) {
           return "None";
        }
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

    String getGameActivityDetails (Activity activity, Guild guild) {
        String details;
        if (activity.getName().equals("Spotify")) {
            RichPresence rp = activity.asRichPresence();
            String spotifyURL = "https://open.spotify.com/track/" + rp.getSyncId();

            details = "[`\"" + rp.getDetails() + "\"";
            if (!(rp.getState() == null)) details +=  " by " + rp.getState();
            details += "`](" + spotifyURL + ") " + Bot.config.get(guild).getEmote().getSpotifyEmote();
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
        if (!(getGameActivity(member) == null)) desc += "\n• " +
                getGameActivityType(getGameActivity(member)) + " " + getGameActivityDetails(getGameActivity(member), member.getGuild());
        desc +=
                "\n\n⌞ Warnings: `" + new Warns().warnCount(member) + "`" +
                "\n⌞ QOTW-Points: `" + new Database().getMemberInt(member, "qotwpoints") +
                        " (#" + new Leaderboard().getQOTWRank(member.getGuild(), member.getId()) + ")`";
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
