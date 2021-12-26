package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamThemeRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamPhase;
import net.javadiscord.javabot.systems.jam.model.JamTheme;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

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
