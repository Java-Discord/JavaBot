package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamTheme;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class ListThemesSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(activeJam);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Themes for Jam " + activeJam.getId())
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")));
		for (JamTheme theme : themes) {
			embedBuilder.addField(theme.getName(), theme.getDescription(), false);
		}
		return event.replyEmbeds(embedBuilder.build()).setEphemeral(true);
	}
}
