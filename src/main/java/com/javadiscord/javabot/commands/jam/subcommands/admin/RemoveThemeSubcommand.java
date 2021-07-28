package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.commands.jam.JamDataManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamPhase;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.List;
import java.util.Objects;

public class RemoveThemeSubcommand extends ActiveJamSubcommand {
	public RemoveThemeSubcommand(JamDataManager dataManager) {
		super(dataManager, true);
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam) throws Exception {
		if (activeJam.getCurrentPhase() == null || !activeJam.getCurrentPhase().equals(JamPhase.THEME_PLANNING)) {
			event.getHook().sendMessage("Themes can only be removed during theme planning.").queue();
			return;
		}

		List<JamTheme> themes = this.dataManager.getThemes(activeJam);
		for (JamTheme theme : themes) {
			if (theme.getName().equals(Objects.requireNonNull(event.getOption("name")).getAsString())) {
				this.dataManager.removeTheme(theme);
				event.getHook().sendMessage("Theme **" + theme.getName() + "** has been removed.").queue();
				return;
			}
		}

		event.getHook().sendMessage("No theme with that name found.").queue();
	}
}
