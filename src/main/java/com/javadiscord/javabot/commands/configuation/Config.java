package com.javadiscord.javabot.commands.configuation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class Config implements SlashCommandHandler {

    public static void setStatsCategory(SlashCommandEvent event, String id) {

        Database.queryConfig(event.getGuild().getId(), "other.stats_category.stats_cid", id);
        event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category ID", "Stats-Category ID succesfully changed to", null, id, true)).queue();
    }

    public static void setStatsMessage(SlashCommandEvent event, String message) {

        Database.queryConfig(event.getGuild().getId(), "other.stats_category.stats_text", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category Message", "Stats-Category Message succesfully changed to", null, message, true)).queue();
    }

    public static void setReportChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfig(event.getGuild().getId(), "channels.report_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Report Channel", "Report Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setLogChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfig(event.getGuild().getId(), "channels.log_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Log Channel", "Log Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setSuggestionChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfig(event.getGuild().getId(), "channels.suggestion_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Suggest Channel", "Suggest Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setSubmissionChannel(SlashCommandEvent event, MessageChannel channel) {

        Database.queryConfig(event.getGuild().getId(), "channels.submission_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "QOTW-Submission Channel", "QOTW-Submission Channel succesfully changed to", null, channel.getId(), true, true)).queue();
    }

    public static void setMuteRole(SlashCommandEvent event, Role role) {

        Database.queryConfig(event.getGuild().getId(), "roles.mute_rid", role.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Mute Role", "Mute Role succesfully changed to", null, role.getId(), true, false, true)).queue();
    }

    public static void setDMQOTWStatus(SlashCommandEvent event, boolean status) {

        Database.queryConfig(event.getGuild().getId(), "other.qotw.dm-qotw", status);
        event.replyEmbeds(Embeds.configEmbed(event, "QOTW-DM Status", "QOTW-DM Status succesfully changed to", null, String.valueOf(status), true)).queue();
    }

    public static void setLockStatus(SlashCommandEvent event, boolean status) {

        Database.queryConfig(event.getGuild().getId(), "other.server_lock.lock_status", status);
        event.replyEmbeds(Embeds.configEmbed(event, "Lock Status changed", "Lock Status succesfully changed to", null, String.valueOf(status))).queue();
    }

    public static void getList(SlashCommandEvent event) {

                var eb = new EmbedBuilder()
                        .setColor(Constants.GRAY)
                        .setTitle("Bot Configuration");

                String overlayURL = Database.getConfigString(event, "welcome_system.image.overlayURL");
                eb.setImage(Misc.checkImage(overlayURL));

                        eb.addField("Lock Status", "Lock: ``" + Database.getConfigBoolean(event, "other.server_lock.lock_status") + "``" +
                                "\nCount: ``" + Database.getConfigInt(event, "other.server_lock.lock_count") + "/5``", true)

                        .addField("Question of the Week", "Submission Channel: " + Database.getConfigChannelAsMention(event, "channels.submission_cid")
                                + "\nSubmission-Status: ``" + Database.getConfigBoolean(event, "other.qotw.dm-qotw") + "``", true)

                        .addField("Stats-Category", "Category-ID: ``" + Database.getConfigString(event, "other.stats_category.stats_cid") + "``" +
                                "\nText: ``" + Database.getConfigString(event, "other.stats_category.stats_text") + "``", false)

                        .addField("Other", "Report Channel: " + Database.getConfigChannelAsMention(event, "channels.report_cid") +
                                ", Log Channel: " + Database.getConfigChannelAsMention(event, "channels.log_cid") +
                                "\nSuggestion Channel: " + Database.getConfigChannelAsMention(event, "channels.suggestion_cid") +
                                ", Mute Role: " + Database.getConfigRoleAsMention(event, "roles.mute_rid"), false);

                event.replyEmbeds(eb.build()).queue();
    }

    @Override
    public void handle(SlashCommandEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            switch (event.getSubcommandName()) {

                case "list":
                    Config.getList(event);
                    break;

                case "stats-category":
                    Config.setStatsCategory(event, event.getOption("id").getAsString());
                    break;

                case "stats-message":
                    Config.setStatsMessage(event, event.getOption("message").getAsString());
                    break;

                case "report-channel":
                    Config.setReportChannel(event, event.getOption("channel").getAsMessageChannel());
                    break;

                case "log-channel":
                    Config.setLogChannel(event, event.getOption("channel").getAsMessageChannel());
                    break;

                case "suggestion-channel":
                    Config.setSuggestionChannel(event, event.getOption("channel").getAsMessageChannel());
                    break;

                case "submission-channel":
                    Config.setSubmissionChannel(event, event.getOption("channel").getAsMessageChannel());
                    break;

                case "mute-role":
                    Config.setMuteRole(event, event.getOption("role").getAsRole());
                    break;

                case "dm-qotw":
                    Config.setDMQOTWStatus(event, event.getOption("enabled").getAsBoolean());
                    break;

                case "lock":
                    Config.setLockStatus(event, event.getOption("locked").getAsBoolean());
                    break;
            }
        } else {
            event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}
