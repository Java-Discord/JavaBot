package net.javadiscord.javabot.systems.jam.subcommands.admin;

import com.dynxsty.dih4jda.interactions.commands.AutoCompletable;
import com.dynxsty.dih4jda.util.AutoCompleteUtils;
import io.sentry.Sentry;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamSubmissionRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamSubmission;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that allows jam-admins to manually remove submissions.
 */
public class RemoveSubmissionsSubcommand extends ActiveJamSubcommand implements AutoCompletable {
	public RemoveSubmissionsSubcommand() {
		setSubcommandData(new SubcommandData("remove-submissions", "Removes one or more submissions from the Jam.")
				.addOption(OptionType.INTEGER, "id", "The id of the submission to remove.", false, true)
				.addOption(OptionType.USER, "user", "The user whose submissions to remove.", false)
		);
		setAutoCompleteHandling(true);
	}

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

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.dataSource.getConnection()) {
			JamRepository jamRepo = new JamRepository(con);
			Jam activeJam = jamRepo.getActiveJam(event.getGuild().getIdLong());
			if (activeJam != null) {
				JamSubmissionRepository submissionRepo = new JamSubmissionRepository(con);
				List<JamSubmission> submissions = submissionRepo.getSubmissions(activeJam).stream().limit(25).toList();
				submissions.forEach(submission ->
						choices.add(new Command.Choice(String.format("Submission by %s", event.getJDA().getUserById(submission.getUserId()).getAsTag()), submission.getId())));
			}
		} catch (SQLException e) {
			Sentry.captureException(e);
			Sentry.captureException(e);
		}
		event.replyChoices(AutoCompleteUtils.filterChoices(event, choices)).queue();
	}
}
