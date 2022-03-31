package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamThemeRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamTheme;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that allows jam-admins to list all added themes.
 */
public class ListThemesSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException {
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(activeJam);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Themes for Jam " + activeJam.getId())
				.setColor(config.getJamEmbedColor());
		for (JamTheme theme : themes) {
			embedBuilder.addField(theme.getName(), theme.getDescription(), false);
		}
		return event.replyEmbeds(embedBuilder.build()).setEphemeral(true);
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
