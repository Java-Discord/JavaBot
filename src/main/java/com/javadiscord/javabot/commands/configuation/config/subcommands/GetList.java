package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Objects;

public class GetList implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Database db = new Database();

        var eb = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setTitle("Bot Configuration")

                .addField("Lock Status", "Locked: `" + db.getConfigBoolean(event.getGuild(), "other.server_lock.lock_status") + "`" +
                        "\nCount: `" + db.getConfigInt(event.getGuild(), "other.server_lock.lock_count") + "/5`", true)

                .addField("Question of the Week", "Submission Channel: " + Bot.config.getQotw().getSubmissionChannel(Objects.requireNonNull(event.getGuild())).getAsMention()
                        + "\nSubmission-Status: `" + Bot.config.getQotw().isDmEnabled() + "`", true)

                .addField("Stats-Category", "Category-ID: `" + db.getConfigString(event.getGuild(), "other.stats_category.stats_cid") + "`" +
                        "\nText: `" + db.getConfigString(event.getGuild(), "other.stats_category.stats_text") + "`", false)

                .addField("Channel & Roles", "Report Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.report_cid") +
                        ", Log Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.log_cid") +
                        "\nSuggestion Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.suggestion_cid") +
                        ", Starboard Channel: " + db.getConfigChannelAsMention(event.getGuild(), "other.starboard.starboard_cid") +
                        "\nJam Announcement Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.jam_announcement_cid") +
                        ", Jam Vote Channel: " + db.getConfigChannelAsMention(event.getGuild(), "channels.jam_vote_cid") +
                        "\n\nMute Role: " + db.getConfigRoleAsMention(event.getGuild(), "roles.mute_rid") +
                        ", Staff Role: " + db.getConfigRoleAsMention(event.getGuild(), "roles.staff_rid") +
                        ", Jam-Admin Role: " + db.getConfigRoleAsMention(event.getGuild(), "roles.jam_admin_rid") +
                        ", Jam-Ping Role: " + db.getConfigRoleAsMention(event.getGuild(), "roles.jam_ping_rid"), false)
                .build();

        return event.replyEmbeds(eb);
    }
}
