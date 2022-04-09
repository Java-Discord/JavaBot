package net.javadiscord.javabot.systems.qotw.submissions.subcommands;

import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows members of the QOTW Review Team to mark a single submission as the "Best Answer" of the current Week.
 */
public class MarkBestAnswerSubcommand implements SlashCommand {
	@Override
	public InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		return null;
	}

	/**
	 * Replies with all accepted Question of the Week Submissions..
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
	public static List<Command.Choice> replyAcceptedSubmissions(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.dataSource.getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			List<QOTWSubmission> questions = repo.getSubmissionByQuestionNumber(repo.getCurrentQuestionNumber());
			questions.forEach(question -> {
				ThreadChannel thread = event.getGuild().getThreadChannelById(question.getThreadId());
				String name = thread == null ? String.valueOf(question.getThreadId()) : thread.getName();
				choices.add(new Command.Choice(name, question.getThreadId()));
			});
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return choices;
	}
}
