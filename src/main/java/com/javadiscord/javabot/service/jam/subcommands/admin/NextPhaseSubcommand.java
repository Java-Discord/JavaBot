package com.javadiscord.javabot.service.jam.subcommands.admin;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.service.jam.JamChannelManager;
import com.javadiscord.javabot.service.jam.JamPhaseManager;
import com.javadiscord.javabot.service.jam.model.Jam;
import com.javadiscord.javabot.service.jam.subcommands.ActiveJamSubcommand;
import com.javadiscord.javabot.data.properties.config.guild.JamConfig;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

public class NextPhaseSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) {
		var channelManager = new JamChannelManager(config);
		new JamPhaseManager(activeJam, event, channelManager).nextPhase();
		return Responses.success(event, "Jam Phase Updated", "The jam's phase is now being updated.");
	}
}
