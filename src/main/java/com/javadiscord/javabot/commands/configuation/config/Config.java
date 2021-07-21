package com.javadiscord.javabot.commands.configuation.config;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.subcommands.*;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.Map;

public class Config implements SlashCommandHandler, ConfigCommandHandler {

    private final Map<String, ConfigCommandHandler> configIndex;

    public Config() {

        this.configIndex = new HashMap<>();

        configIndex.put("list", new GetList());
        configIndex.put("stats-category", new SetStatsCategory());
        configIndex.put("stats-message", new SetStatsMessage());
        configIndex.put("report-channel",new SetReportChannel());
        configIndex.put("starboard-channel",new SetStarboardChannel());
        configIndex.put("log-channel", new SetLogChannel());
        configIndex.put("suggestion-channel", new SetSuggestionChannel());
        configIndex.put("submission-channel", new SetSubmissionChannel());
        configIndex.put("mute-role", new SetMuteRole());
        configIndex.put("dm-qotw", new SetDMQOTWStatus());
        configIndex.put("lock", new SetLockStatus());
    }

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        var command = configIndex.get(event.getSubcommandName());
        if (command != null) {
            command.handle(event);
            return;
        }
    }
}

