package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamThemeRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamTheme;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Subcommand that allows jam-admins to list all added themes.
 */
public class ListThemesSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException {
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(activeJam);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Themes for Jam " + activeJam.getId())
				.setColor(config.getJamEmbedColor());
		for (JamTheme theme : themes) {
			embedBuilder.addField(theme.getName(), theme.getDescription(), false);
		}
		return event.replyEmbeds(embedBuilder.build()).setEphemeral(true);
	}
}
