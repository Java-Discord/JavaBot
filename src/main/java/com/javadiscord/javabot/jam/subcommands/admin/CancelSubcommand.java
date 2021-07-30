package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.jam.dao.JamRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.Connection;

/**
 * Subcommand which cancels the current Java Jam.
 */
public class CancelSubcommand extends ActiveJamSubcommand {
	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		OptionMapping confirmOption = event.getOption("confirm");
		if (confirmOption == null || !confirmOption.getAsString().equals("yes")) {
			throw new IllegalArgumentException("Invalid confirmation. Type 'yes' to confirm cancellation.");
		}
		event.getHook().sendMessage("Cancelling the current Java Jam...").queue();

		new JamRepository(con).cancelJam(activeJam);

		TextChannel announcementChannel = new Database().getConfigChannel(event.getGuild(), "channels.jam_announcement_cid");
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");
		announcementChannel.sendMessage("The current Java Jam has been cancelled.").queue();
	}
}
