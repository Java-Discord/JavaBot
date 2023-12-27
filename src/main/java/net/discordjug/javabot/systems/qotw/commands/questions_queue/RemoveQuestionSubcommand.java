package net.discordjug.javabot.systems.qotw.commands.questions_queue;

import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;
import net.discordjug.javabot.systems.qotw.commands.QOTWSubcommand;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWQuestion;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that allows staff-members to remove single questions from the QOTW Queue.
 */
public class RemoveQuestionSubcommand extends QOTWSubcommand implements AutoCompletable {
	private final QuestionQueueRepository questionQueueRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param questionQueueRepository Dao class that represents the QOTW_QUESTION SQL Table.
	 */
	public RemoveQuestionSubcommand(QuestionQueueRepository questionQueueRepository) {
		this.questionQueueRepository = questionQueueRepository;
		setCommandData(new SubcommandData("remove", "Removes a question from the queue.")
				.addOption(OptionType.INTEGER, "id", "The id of the question to remove.", true, true)
		);
	}

	@Override
	protected InteractionCallbackAction<?> handleCommand(SlashCommandInteractionEvent event, long guildId) throws DataAccessException {
		OptionMapping idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.replyMissingArguments(event);
		}
		long id = idOption.getAsLong();
		boolean removed = questionQueueRepository.removeQuestion(guildId, id);
		if (removed) {
			return Responses.success(event, "Question Removed", "The question with id `" + id + "` has been removed.");
		} else {
			return Responses.warning(event, "Could not remove question with id `" + id + "`. Are you sure it exists?");
		}
	}

	/**
	 * Replies with all Question of the Week Questions.
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
	public List<Command.Choice> replyQuestions(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try {
			List<QOTWQuestion> questions = questionQueueRepository.getQuestions(event.getGuild().getIdLong(), 0, 25);
			questions.forEach(question -> choices.add(new Command.Choice(String.format("(Priority: %s) %s", question.getPriority(), question.getText()), question.getId())));
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, RemoveQuestionSubcommand.class.getSimpleName());
		}
		return choices;
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		event.replyChoices(AutoCompleteUtils.filterChoices(event, replyQuestions(event))).queue();
	}
}
