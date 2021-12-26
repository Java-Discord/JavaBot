package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.mongodb.Database;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.util.TimeUtils;

import java.awt.*;

public class ProfileCommand implements SlashCommandHandler {

    private String getBadges (Member member) {
        String badges = "";
        var config = Bot.config.get(member.getGuild()).getEmote();
        if (member.getUser().isBot()) badges += config.getBotEmote().getAsMention();
        if (member.getTimeBoosted() != null) badges += config.getServerBoostEmote().getAsMention();

        badges += member.getUser().getFlags().toString()
                .substring(1, member.getUser().getFlags().toString().length() - 1)
                .replace(",", "")
                .replace("PARTNER", config.getPartnerEmote().getAsMention())
                .replace("HYPESQUAD_BRAVERY", config.getBraveryEmote().getAsMention())
                .replace("HYPESQUAD_BRILLIANCE", config.getBrillianceEmote().getAsMention())
                .replace("HYPESQUAD_BALANCE", config.getBalanceEmote().getAsMention())
                .replace("VERIFIED_DEVELOPER", config.getDevEmote().getAsMention())
                .replace("EARLY_SUPPORTER", config.getEarlySupporterEmote().getAsMention())
                .replace("SYSTEM", config.getStaffEmote().getAsMention())
                .replace("BUG_HUNTER_LEVEL_1", config.getBugHunterEmote().getAsMention())
                .replace("BUG_HUNTER_LEVEL_2", config.getBugHunterEmote().getAsMention())
                .replace("VERIFIED_BOT", "");
        return badges;
    }

    private String getOnlineStatus (Member member) {
        var config = Bot.config.get(member.getGuild()).getEmote();
        return member.getOnlineStatus().toString()
                .replace("ONLINE", config.getOnlineEmote().getAsMention())
                .replace("IDLE", config.getIdleEmote().getAsMention())
                .replace("DO_NOT_DISTURB", config.getDndEmote().getAsMention())
                .replace("OFFLINE", config.getOfflineEmote().getAsMention());
    }

    private Color getColor (Member member) {
        if (member.getColor() == null) return Bot.config.get(member.getGuild()).getSlashCommand().getDefaultColor();
        else return member.getColor();
    }

    private String getColorRoleAsMention (Member member) {
        try {
            return member.getRoles().get(0).getAsMention();
        } catch (IndexOutOfBoundsException e) {
           return "None";
        }
    }

    private String getColorAsHex (Color color) {
        return "#" + Integer.toHexString(color.getRGB()).toUpperCase();
    }

    private Activity getCustomActivity (Member member) {
        Activity activity = null;
        for (var act : member.getActivities()) {
            if (act.getType().name().equals("CUSTOM_STATUS")) {
                activity = act;
                break;
            }
        }
        return activity;
    }

    private Activity getGameActivity (Member member) {
        Activity activity = null;
        for (var act : member.getActivities()) {
            if (act.getType().name().equals("CUSTOM_STATUS")) continue;
            else activity = act; break;
        }
        return activity;
    }

    private String getGameActivityType (Activity activity) {
        return activity.getType().name().toLowerCase()
                .replace("listening", "Listening to")
                .replace("default", "Playing");
    }

    private String getGameActivityDetails (Activity activity, Guild guild) {
        String details;
        if (activity.getName().equals("Spotify")) {
            RichPresence rp = activity.asRichPresence();
            String spotifyURL = "https://open.spotify.com/track/" + rp.getSyncId();

            details = "[`\"" + rp.getDetails() + "\"";
            if (rp.getState() != null) details +=  " by " + rp.getState();
            details += "`](" + spotifyURL + ") " + Bot.config.get(guild).getEmote().getSpotifyEmote().getAsMention();
        } else details = "`" + activity.getName() + "`";
        return details;
    }

    private String getServerJoinedDate (Member member, TimeUtils tu) {
        long timeJoined = member.getTimeJoined().toInstant().getEpochSecond();
        String joinDiff = tu.formatDurationToNow(member.getTimeJoined());
        return "<t:" + timeJoined + ":F>" + " (" + joinDiff + " ago)";
    }

    private String getAccountCreatedDate (Member member, TimeUtils tu) {
        long timeCreated = member.getTimeCreated().toInstant().getEpochSecond();
        String createDiff = tu.formatDurationToNow(member.getTimeCreated());
        return "<t:" + timeCreated + ":F>" + " (" + createDiff + " ago)";
    }

    private String getDescription (Member member) {
        String desc = "";
        if (getCustomActivity(member) != null) {
            desc += "\n\"" + getCustomActivity(member).getName() + "\"";
        }
        if (getGameActivity(member) != null) {
            desc += String.format("\n• %s %s", getGameActivityType(getGameActivity(member)), getGameActivityDetails(getGameActivity(member), member.getGuild()));
        }
        desc +=
                "\n\n⌞ Warnings: `" + new ModerationService(member.getJDA(), Bot.config.get(member.getGuild()).getModeration())
                        .getWarns(member.getIdLong()).size() + "`" +
                "\n⌞ QOTW-Points: `" + new Database().getMemberInt(member, "qotwpoints") +
                        " (#" + new LeaderboardCommand().getQOTWRank(member.getGuild(), member.getId()) + ")`";
        return desc;
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping profileOption = event.getOption("user");
        Member member = profileOption == null || profileOption.getAsMember() == null ? event.getMember() : profileOption.getAsMember();
        TimeUtils tu = new TimeUtils();

        var e = new EmbedBuilder()
            .setTitle(getOnlineStatus(member) + " " + member.getUser().getAsTag() + " " + getBadges(member))
            .setThumbnail(member.getUser().getEffectiveAvatarUrl() + "?size=4096")
            .setColor(getColor(member))
            .setDescription(getDescription(member))
            .setFooter("ID: " + member.getId());

            if (member.getRoles().size() > 1 ) e.addField("Roles", getColorRoleAsMention(member) + " (+" + (member.getRoles().size() -1) + " other)", true);
            else if (!member.getRoles().isEmpty()) e.addField("Roles", getColorRoleAsMention(member), true);

            if (!member.getRoles().isEmpty()) e.addField("Color", "`" + getColorAsHex(getColor(member)) + "`", true);
            e.addField("Server joined on", getServerJoinedDate(member, tu), false)
                .addField("Account created on", getAccountCreatedDate(member, tu), true);
            return event.replyEmbeds(e.build());
    }
}
