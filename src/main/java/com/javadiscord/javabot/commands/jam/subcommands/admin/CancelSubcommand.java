package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.Connection;

/**
 * Subcommand which cancels the current Java Jam.
 */
public class CancelSubcommand extends ActiveJamSubcommand {
	public CancelSubcommand() {
		super(true);
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		OptionMapping confirmOption = event.getOption("confirm");
		if (confirmOption == null || !confirmOption.getAsString().equals("yes")) {
			throw new IllegalArgumentException("Invalid confirmation. Type 'yes' to confirm cancellation.");
		}
		event.getHook().sendMessage("Cancelling the current Java Jam...").queue();

		new JamRepository(con).cancelJam(activeJam);

		TextChannel announcementChannel = event.getJDA().getTextChannelById(Bot.getProperty("jamAnnouncementChannelId"));
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");
		announcementChannel.sendMessage("The current Java Jam has been cancelled.").queue();
	}
}
