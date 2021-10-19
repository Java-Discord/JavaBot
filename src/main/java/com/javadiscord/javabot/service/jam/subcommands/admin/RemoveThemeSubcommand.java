package com.javadiscord.javabot.service.jam.subcommands.admin;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.data.properties.config.guild.JamConfig;
import com.javadiscord.javabot.service.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.service.jam.model.Jam;
import com.javadiscord.javabot.service.jam.model.JamPhase;
import com.javadiscord.javabot.service.jam.model.JamTheme;
import com.javadiscord.javabot.service.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

public class RemoveThemeSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws Exception {
		if (activeJam.getCurrentPhase() == null || !activeJam.getCurrentPhase().equals(JamPhase.THEME_PLANNING)) {
			return Responses.warning(event, "Themes can only be removed during theme planning.");
		}

		JamThemeRepository themeRepository = new JamThemeRepository(con);
		List<JamTheme> themes = themeRepository.getThemes(activeJam);
		for (JamTheme theme : themes) {
			if (theme.getName().equals(Objects.requireNonNull(event.getOption("name")).getAsString())) {
				themeRepository.removeTheme(theme);
				return Responses.success(event, "Theme Removed", "Theme **" + theme.getName() + "** has been removed.");
			}
		}

		return Responses.warning(event, "Theme Not Found", "No theme with that name was found.");
	}
}
