package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.commands.jam.JamDataManager;
import com.javadiscord.javabot.commands.jam.JamPhaseManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamPhase;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NextPhaseSubcommand extends ActiveJamSubcommand {
	private final JamPhaseManager phaseManager;
	private final ExecutorService executorService;

	public NextPhaseSubcommand(JamDataManager dataManager) {
		super(dataManager, true);
		this.phaseManager = new JamPhaseManager(dataManager);
		this.executorService = Executors.newSingleThreadExecutor();
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam) throws Exception {
		String previousPhase = activeJam.getCurrentPhase();
		if (previousPhase == null) {
			event.getHook().sendMessage("Jam is not in any phase.").queue();
			return;
		}
		if (previousPhase.equals(JamPhase.THEME_PLANNING)) {
			this.phaseManager.moveToThemeVoting(activeJam, event);
		}
		event.getHook().sendMessage(String.format("Moved Jam from %s phase to %s phase.", previousPhase, activeJam.getCurrentPhase())).queue();
	}
}
