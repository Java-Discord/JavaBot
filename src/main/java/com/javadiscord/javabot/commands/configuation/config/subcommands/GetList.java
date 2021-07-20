package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class GetList implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        Database db = new Database();

        var eb = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setTitle("Bot Configuration")

                .addField("Lock Status", "Lock: `" + db.getConfigBoolean(event.getGuild(), "other.server_lock.lock_status") + "`" +
                        "\nCount: `" + db.getConfigInt(event.getGuild(), "other.server_lock.lock_count") + "/5`", true)

                .addField("Question of the Week", "Submission Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.submission_cid")
                        + "\nSubmission-Status: `" + db.getConfigBoolean(event.getGuild(), "other.qotw.dm-qotw") + "`", true)

                .addField("Stats-Category", "Category-ID: `" + db.getConfigString(event.getGuild(), "other.stats_category.stats_cid") + "`" +
                        "\nText: `" + db.getConfigString(event.getGuild(), "other.stats_category.stats_text") + "`", false)

                .addField("Other", "Report Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.report_cid") +
                        ", Log Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.log_cid") +
                        "\nSuggestion Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.suggestion_cid") +
                        ", Mute Role: " + db.getConfigRoleAsMention(event.getGuild(), "roles.mute_rid"), false)
                .build();

        event.replyEmbeds(eb).queue();
    }
}
