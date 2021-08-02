package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.jam.JamChannelManager;
import com.javadiscord.javabot.jam.JamPhaseManager;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

public class NextPhaseSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) {
		var channelManager = new JamChannelManager(event.getGuild(), new Database());
		new JamPhaseManager(activeJam, event, channelManager).nextPhase();
		return Responses.success(event, "Jam Phase Updated", "The jam's phase is now being updated.");
	}
}
