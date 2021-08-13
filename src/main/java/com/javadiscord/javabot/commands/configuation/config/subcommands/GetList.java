package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class GetList implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var eb = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setTitle("Bot Configuration")

                .addField("Lock Status", "Locked: `" + Database.getConfigBoolean(event.getGuild(), "other.server_lock.lock_status") + "`" +
                        "\nCount: `" + Database.getConfigInt(event.getGuild(), "other.server_lock.lock_count") + "/5`", true)

                .addField("Question of the Week", "Submission Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "channels.submission_cid")
                        + "\nSubmission-Status: `" + Database.getConfigBoolean(event.getGuild(), "other.qotw.dm-qotw") + "`", true)

                .addField("Stats-Category", "Category-ID: `" + Database.getConfigString(event.getGuild(), "other.stats_category.stats_cid") + "`" +
                        "\nText: `" + Database.getConfigString(event.getGuild(), "other.stats_category.stats_text") + "`", false)

                .addField("Channel & Roles", "Report Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "channels.report_cid") +
                        ", Log Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "channels.log_cid") +
                        "\nSuggestion Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "channels.suggestion_cid") +
                        ", Starboard Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "other.starboard.starboard_cid") +
                        "\nJam Announcement Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "channels.jam_announcement_cid") +
                        ", Jam Vote Channel: " + Database.getConfigChannelAsMention(event.getGuild(), "channels.jam_vote_cid") +
                        "\n\nMute Role: " + Database.getConfigRoleAsMention(event.getGuild(), "roles.mute_rid") +
                        ", Staff Role: " + Database.getConfigRoleAsMention(event.getGuild(), "roles.staff_rid") +
                        ", Jam-Admin Role: " + Database.getConfigRoleAsMention(event.getGuild(), "roles.jam_admin_rid") +
                        ", Jam-Ping Role: " + Database.getConfigRoleAsMention(event.getGuild(), "roles.jam_ping_rid"), false)
                .build();

        return event.replyEmbeds(eb);
    }
}
