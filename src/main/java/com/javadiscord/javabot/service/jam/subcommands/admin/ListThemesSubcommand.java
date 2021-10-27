package com.javadiscord.javabot.service.jam.subcommands.admin;

import com.javadiscord.javabot.data.properties.config.guild.JamConfig;
import com.javadiscord.javabot.service.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.service.jam.model.Jam;
import com.javadiscord.javabot.service.jam.model.JamTheme;
import com.javadiscord.javabot.service.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;
import java.util.List;

public class ListThemesSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws Exception {
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
