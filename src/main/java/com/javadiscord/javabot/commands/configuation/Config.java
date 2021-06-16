package com.javadiscord.javabot.commands.configuation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class Config {

    public static void setLeaveMessage(SlashCommandEvent event, String message) {

        Database.queryConfigString(event.getGuild().getId(), "leave_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Leave Message", "Leave Message succesfully changed to", null, message, true)).queue();
    }

    public static void setWelcomeMessage(SlashCommandEvent event, String message) {

        Database.queryConfigString(event.getGuild().getId(), "welcome_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Message", "Welcome Message succesfully changed to", null, message, true)).queue();
    }

    public static void setWelcomeChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfigString(event.getGuild().getId(), "welcome_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Channel", "Welcome Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setStatsCategory(SlashCommandEvent event, String id) {

        Database.queryConfigString(event.getGuild().getId(), "stats_cid", id);
        event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category ID", "Stats-Category ID succesfully changed to", null, id, true)).queue();
    }

    public static void setStatsMessage(SlashCommandEvent event, String message) {

        Database.queryConfigString(event.getGuild().getId(), "stats_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category Message", "Stats-Category Message succesfully changed to", null, message, true)).queue();
    }

    public static void setReportChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfigString(event.getGuild().getId(), "report_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Report Channel", "Report Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setLogChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfigString(event.getGuild().getId(), "log_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Log Channel", "Log Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setSuggestionChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfigString(event.getGuild().getId(), "suggestion_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Suggest Channel", "Suggest Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setSubmissionChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfigString(event.getGuild().getId(), "submission_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "QOTW-Submission Channel", "QOTW-Submission Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setMuteRole(SlashCommandEvent event, Role role) {

        Database.queryConfigString(event.getGuild().getId(), "mute_rid", role.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Mute Role", "Mute Role succesfully changed to", null, role.getId(), true, false, true)).queue();
    }

    public static void setDMQOTWStatus(SlashCommandEvent event, boolean status) {

        Database.queryConfigString(event.getGuild().getId(), "dm-qotw", String.valueOf(status));
        event.replyEmbeds(Embeds.configEmbed(event, "QOTW-DM Status", "QOTW-DM Status succesfully changed to", null, String.valueOf(status), true)).queue();
    }

    public static void setLockStatus(SlashCommandEvent event, boolean status) {

        Database.queryConfigString(event.getGuild().getId(), "lock", String.valueOf(status));
        event.replyEmbeds(Embeds.configEmbed(event, "Lock Status changed", "Lock Status succesfully changed to", null, String.valueOf(status))).queue();
    }

    public static void getList(SlashCommandEvent event) {

                var eb = new EmbedBuilder()
                        .setColor(Constants.GRAY)
                        .setTitle("Bot Configuration");

                String overlayURL = Database.welcomeImage(event.getGuild().getId()).get("overlayURL").getAsString();
                eb.setImage(Misc.checkImage(overlayURL));

                        eb.addField("Server locked?", "``" + Database.getConfigString(event, "lock") + ", " + Database.getConfigString(event, "lockcount") + "``", false)
                        .addField("QOTW", "<#" + Database.getConfigString(event, "submission_cid") + ">\n" + "DM-Submissions: ``" + Database.getConfigString(event, "dm-qotw") + "``", false)
                        .addField("Stats-Category", Database.getConfigString(event, "stats_cid") + "\n``" + Database.getConfigString(event, "stats_msg") + "``", true)
                        .addField("Report", "<#" + Database.getConfigString(event, "report_cid") + ">", true)
                        .addField("Log", "<#" + Database.getConfigString(event, "log_cid") + ">", true)
                        .addField("Mute", "<@&" + Database.getConfigString(event, "mute_rid") + ">", true)
                        .addField("Suggestions", "<#" + Database.getConfigString(event, "suggestion_cid") + ">", true)
                        .addField("Welcome-System",
                                "<#" + Database.getConfigString(event, "welcome_cid") +
                                ">\nWelcome Message: ``" + Database.getConfigString(event, "welcome_msg") +
                                "``\nLeave Message: ``" + Database.getConfigString(event, "leave_msg") +
                                "``\n[Image Link](" + overlayURL + ")", false);

                event.replyEmbeds(eb.build()).queue();
    }
}
