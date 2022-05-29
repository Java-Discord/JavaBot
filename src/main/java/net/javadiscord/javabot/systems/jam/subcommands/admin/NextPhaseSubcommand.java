package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.JamChannelManager;
import net.javadiscord.javabot.systems.jam.JamPhaseManager;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;

/**
 * Subcommand that allows jam-admin to manually forward to the next jam phase.
 */
public class NextPhaseSubcommand extends ActiveJamSubcommand {
	public NextPhaseSubcommand() {
		setSubcommandData(new SubcommandData("next-phase", "Manually moves the currently active Jam to the next phase."));
	}

	@Override
	protected ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) {
		var channelManager = new JamChannelManager(config);
		new JamPhaseManager(activeJam, event, channelManager).nextPhase();
		return Responses.success(event, "Jam Phase Updated", "The jam's phase is now being updated.");
	}
}
