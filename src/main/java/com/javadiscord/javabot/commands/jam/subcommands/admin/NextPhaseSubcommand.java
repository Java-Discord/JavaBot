package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.commands.jam.JamPhaseManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;

public class NextPhaseSubcommand extends ActiveJamSubcommand {
	public NextPhaseSubcommand() {
		super(true);
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		String previousPhase = activeJam.getCurrentPhase();
		if (previousPhase == null) {
			event.getHook().sendMessage("Jam is not in any phase.").queue();
			return;
		}
		JamPhaseManager phaseManager = new JamPhaseManager(con);
		phaseManager.nextPhase(activeJam, event);
		event.getHook().sendMessage("Moved jam to next phase.").queue();
	}
}
