package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class GetList implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        var eb = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setTitle("Bot Configuration")

                .addField("Lock Status", "Lock: `" + Database.getConfigBoolean(event, "other.server_lock.lock_status") + "`" +
                        "\nCount: `" + Database.getConfigInt(event, "other.server_lock.lock_count") + "/5`", true)

                .addField("Question of the Week", "Submission Channel: " + Database.getConfigChannelAsMention(event, "channels.submission_cid")
                        + "\nSubmission-Status: `" + Database.getConfigBoolean(event, "other.qotw.dm-qotw") + "`", true)

                .addField("Stats-Category", "Category-ID: `" + Database.getConfigString(event, "other.stats_category.stats_cid") + "`" +
                        "\nText: `" + Database.getConfigString(event, "other.stats_category.stats_text") + "`", false)

                .addField("Other", "Report Channel: " + Database.getConfigChannelAsMention(event, "channels.report_cid") +
                        ", Log Channel: " + Database.getConfigChannelAsMention(event, "channels.log_cid") +
                        "\nSuggestion Channel: " + Database.getConfigChannelAsMention(event, "channels.suggestion_cid") +
                        ", Mute Role: " + Database.getConfigRoleAsMention(event, "roles.mute_rid"), false)
                .build();

        event.replyEmbeds(eb).queue();
    }
}
