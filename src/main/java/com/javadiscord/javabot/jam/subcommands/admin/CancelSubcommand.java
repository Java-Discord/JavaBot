package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.jam.dao.JamRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import com.javadiscord.javabot.properties.config.guild.JamConfig;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

/**
 * Subcommand which cancels the current Java Jam.
 */
public class CancelSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws Exception {
		OptionMapping confirmOption = event.getOption("confirm");
		if (confirmOption == null || !confirmOption.getAsString().equals("yes")) {
			return Responses.warning(event, "Invalid confirmation. Type `yes` to confirm cancellation.");
		}
		TextChannel announcementChannel = config.getAnnouncementChannel();
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");

		new JamRepository(con).cancelJam(activeJam);
		announcementChannel.sendMessage("The current Java Jam has been cancelled.").queue();

		return Responses.success(event, "Jam Cancelled", "The " + activeJam.getFullName() + " has been cancelled.");
	}
}
