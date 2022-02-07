package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamSubmissionRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Subcommand that allows jam-admins to manually remove submissions.
 */
public class RemoveSubmissionsSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException {
		OptionMapping idOption = event.getOption("id");
		OptionMapping userOption = event.getOption("user");
		if (idOption == null && userOption == null) {
			return Responses.warning(event, "Either a submission id or user must be provided.");
		}
		if (idOption != null && userOption != null) {
			return Responses.warning(event, "Provide only a submission id or user, not both.");
		}

		JamSubmissionRepository submissionRepository = new JamSubmissionRepository(con);
		int removed;
		if (idOption != null) {
			removed = submissionRepository.removeSubmission(activeJam, idOption.getAsLong());
		} else {
			removed = submissionRepository.removeSubmissions(activeJam, userOption.getAsUser().getIdLong());
		}
		return Responses.success(event, "Submissions Removed", "Removed " + removed + " submissions from the " + activeJam.getFullName() + ".");
	}
}
