package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.entities.NewsChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;
import net.javadiscord.javabot.util.Responses;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Subcommand which cancels the current Java Jam.
 */
public class CancelSubcommand extends ActiveJamSubcommand {
	public CancelSubcommand() {
		setSubcommandData(new SubcommandData("cancel", "Cancels the current Jam. Use with caution!")
				.addOption(OptionType.STRING, "confirm", "Type 'yes' to confirm that you want to cancel the Jam.", true));
	}

	@Override
	protected ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException {
		OptionMapping confirmOption = event.getOption("confirm");
		if (confirmOption == null || !confirmOption.getAsString().equals("yes")) {
			return Responses.warning(event, "Invalid confirmation. Type `yes` to confirm cancellation.");
		}
		NewsChannel announcementChannel = config.getAnnouncementChannel();
		if (announcementChannel == null) throw new IllegalArgumentException("Invalid jam announcement channel id.");

		new JamRepository(con).cancelJam(activeJam);
		announcementChannel.sendMessage("The current Java Jam has been cancelled.").queue();

		return Responses.success(event, "Jam Cancelled", "The " + activeJam.getFullName() + " has been cancelled.");
	}
}
