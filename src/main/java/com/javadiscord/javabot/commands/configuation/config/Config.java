package com.javadiscord.javabot.commands.configuation.config;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.configuation.config.subcommands.*;
import com.javadiscord.javabot.other.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Date;

// TODO: Replace with file-based config or at least something much less convoluted.
@Deprecated
public class Config extends DelegatingCommandHandler {
    public Config() {
        addSubcommand("list", new GetList());
        addSubcommand("stats-category", new SetStatsCategory());
        addSubcommand("stats-message", new SetStatsMessage());
        addSubcommand("report-channel", new SetReportChannel());
        addSubcommand("starboard-channel", new SetStarboardChannel());
        addSubcommand("log-channel", new SetLogChannel());
        addSubcommand("suggestion-channel", new SetSuggestionChannel());
        addSubcommand("submission-channel", new SetSubmissionChannel());
        addSubcommand("mute-role", new SetMuteRole());
        addSubcommand("staff-role", new SetStaffRole());
        addSubcommand("dm-qotw", new SetDMQOTWStatus());
        addSubcommand("lock", new SetLockStatus());

        addSubcommand("jam-admin-role", new SetJamAdminRole());
        addSubcommand("jam-ping-role", new SetJamPingRole());
        addSubcommand("jam-vote-channel", new SetJamVoteChannel());
        addSubcommand("jam-announcement-channel", new SetJamAnnouncementChannel());
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        return Responses.error(event, "This command is currently not available.");

        //try { return super.handle(event);
        //} catch (Exception e) { return Responses.error(event, "```" + e.getMessage() + "```"); }
    }

    public MessageEmbed configEmbed (String configName, String newValue) {
        var embed = new EmbedBuilder()
                .setColor(Constants.GRAY)
                .setAuthor("Config: " + configName)
                .setDescription("Successfully set ``" + configName + "`` to " + newValue)
                .setTimestamp(new Date().toInstant())
                .build();
        return embed;
    }
}

