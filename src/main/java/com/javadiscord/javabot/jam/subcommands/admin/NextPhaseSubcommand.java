package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.jam.JamPhaseManager;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;

public class NextPhaseSubcommand extends ActiveJamSubcommand {
	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) {
		String previousPhase = activeJam.getCurrentPhase();
		if (previousPhase == null) {
			event.getHook().sendMessage("Jam is not in any phase.").queue();
			return;
		}
		JamPhaseManager phaseManager = new JamPhaseManager();
		phaseManager.nextPhase(activeJam, event);
		event.getHook().sendMessage("Moved jam to next phase.").queue();
	}
}
