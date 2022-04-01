package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamThemeRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamPhase;
import net.javadiscord.javabot.systems.jam.model.JamTheme;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Subcommand that allows jam-admins to manually remove themes.
 */
public class RemoveThemeSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException {
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

	/**
	 * Replies with all jam themes.
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return The {@link AutoCompleteCallbackAction}.
	 */
	public static AutoCompleteCallbackAction replyThemes(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.dataSource.getConnection()) {
			JamRepository jamRepo = new JamRepository(con);
			Jam activeJam = jamRepo.getActiveJam(event.getGuild().getIdLong());
			if (activeJam != null) {
				JamThemeRepository repo = new JamThemeRepository(con);
				List<JamTheme> themes = repo.getThemes(activeJam).stream().limit(25).toList();
				themes.forEach(theme -> choices.add(new Command.Choice(theme.getName(), theme.getName())));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return event.replyChoices(choices);
	}
}
