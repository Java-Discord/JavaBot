package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.jam.JamCommandHandler;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@RequiredArgsConstructor
public class PlanNewJamSubcommand implements SlashCommandHandler {
	@Override
	public void handle(SlashCommandEvent event) {
		if (!JamCommandHandler.ensureAdmin(event)) return;
		event.deferReply().queue();
		OptionMapping startOption = event.getOption("start-date");
		if (startOption == null) {
			event.getHook().sendMessage("Missing start date.").queue();
			return;
		}
		LocalDate startsAt;
		try {
			startsAt = LocalDate.parse(startOption.getAsString(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		} catch (DateTimeParseException e) {
			event.getHook().sendMessage("Invalid start date. Must be formatted as `dd-MM-yyyy`. For example, June 5th, 2017 is formatted as `05-06-2017`.").queue();
			return;
		}
		if (startsAt.isBefore(LocalDate.now())) {
			event.getHook().sendMessage("Invalid start date. The Jam cannot start in the past.").queue();
			return;
		}
		long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
		String name = null;
		OptionMapping nameOption = event.getOption("name");
		if (nameOption != null) {
			name = nameOption.getAsString();
		}

		try {
			Connection con = Bot.dataSource.getConnection();
			JamRepository jamRepository = new JamRepository(con);

			Jam activeJam = jamRepository.getActiveJam(guildId);
			if (activeJam != null) {
				event.getHook().sendMessage("There is already an active Jam (id = `" + activeJam.getId() + "`). Complete that Jam before planning a new one.").queue();
				return;
			}

			Jam jam = new Jam();
			jam.setGuildId(guildId);
			jam.setName(name);
			jam.setStartedBy(event.getUser().getIdLong());
			jam.setStartsAt(startsAt);
			jam.setCompleted(false);
			jamRepository.saveJam(jam);
			event.getHook().sendMessage("Jam has been created! *Jam ID = `" + jam.getId() + "`*. Use `/jam info` for more info.").queue();
		} catch (Exception e) {
			event.getHook().sendMessage("Error occurred while creating the Jam: " + e.getMessage()).queue();
		}
	}
}
