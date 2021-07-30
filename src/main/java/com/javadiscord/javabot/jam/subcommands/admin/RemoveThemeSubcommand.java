package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamPhase;
import com.javadiscord.javabot.jam.model.JamTheme;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

public class RemoveThemeSubcommand extends ActiveJamSubcommand {
	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		if (activeJam.getCurrentPhase() == null || !activeJam.getCurrentPhase().equals(JamPhase.THEME_PLANNING)) {
			event.getHook().sendMessage("Themes can only be removed during theme planning.").queue();
			return;
		}

		JamThemeRepository themeRepository = new JamThemeRepository(con);
		List<JamTheme> themes = themeRepository.getThemes(activeJam);
		for (JamTheme theme : themes) {
			if (theme.getName().equals(Objects.requireNonNull(event.getOption("name")).getAsString())) {
				themeRepository.removeTheme(theme);
				event.getHook().sendMessage("Theme **" + theme.getName() + "** has been removed.").queue();
				return;
			}
		}

		event.getHook().sendMessage("No theme with that name found.").queue();
	}
}
