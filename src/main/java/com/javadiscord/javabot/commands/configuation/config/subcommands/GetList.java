package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class GetList implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Database db = new Database();

        var c = Bot.config.get(event.getGuild());
        var eb = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setTitle("Bot Configuration")

                //.addField("Lock Status", "Locked: `" + db.getConfigBoolean(event.getGuild(), "other.server_lock.lock_status") + "`" +
                //        "\nCount: `" + db.getConfigInt(event.getGuild(), "other.server_lock.lock_count") + "/5`", true)

                .addField("Question of the Week", "Submission Channel: " + c.getQotw().getSubmissionChannel().getAsMention()
                        + "\nSubmission-Status: `" + c.getQotw().isDmEnabled() + "`", true)

                .addField("Stats-Category", "Category-ID: `" + c.getStats().getCategoryId() + "`" +
                        "\nText: `" + c.getStats().getMemberCountMessageTemplate() + "`", false)

                .addField("Channel & Roles", "Report Channel: " + c.getModeration().getReportChannel().getAsMention() +
                        ", Log Channel: " + c.getModeration().getLogChannel().getAsMention() +
                        "\nSuggestion Channel: " + c.getModeration().getSuggestionChannel().getAsMention() +
                        ", Starboard Channel: " + c.getStarBoard().getChannel().getAsMention() +
                        "\nJam Announcement Channel: " + c.getJam().getAnnouncementChannel().getAsMention() +
                        ", Jam Vote Channel: " + c.getJam().getVotingChannel().getAsMention() +
                        "\n\nMute Role: " + c.getModeration().getMuteRole().getAsMention() +
                        ", Staff Role: " + c.getModeration().getStaffRole().getAsMention() +
                        ", Jam-Admin Role: " + c.getJam().getAdminRole().getAsMention() +
                        ", Jam-Ping Role: " + c.getJam().getPingRole().getAsMention(), false)
                .build();

        return event.replyEmbeds(eb);
    }
}
