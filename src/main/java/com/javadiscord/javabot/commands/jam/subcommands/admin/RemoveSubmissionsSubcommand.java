package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.commands.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.Connection;

public class RemoveSubmissionsSubcommand extends ActiveJamSubcommand {
	public RemoveSubmissionsSubcommand() {
		super(true);
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		OptionMapping idOption = event.getOption("id");
		OptionMapping userOption = event.getOption("user");
		if (idOption == null && userOption == null) throw new IllegalArgumentException("Either a submission id or user must be provided.");
		if (idOption != null && userOption != null) throw new IllegalArgumentException("Provide only a submission id or user, not both.");

		JamSubmissionRepository submissionRepository = new JamSubmissionRepository(con);
		int removed;
		if (idOption != null) {
			removed = submissionRepository.removeSubmission(activeJam, idOption.getAsLong());
		} else {
			removed = submissionRepository.removeSubmissions(activeJam, userOption.getAsUser().getIdLong());
		}
		event.getHook().sendMessage("Removed " + removed + " submissions.").queue();
	}
}
