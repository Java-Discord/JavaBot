package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import com.javadiscord.javabot.properties.config.guild.JamConfig;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

public class RemoveSubmissionsSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws Exception {
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
