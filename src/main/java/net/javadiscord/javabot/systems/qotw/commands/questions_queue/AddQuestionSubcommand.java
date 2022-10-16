package net.javadiscord.javabot.systems.qotw.commands.questions_queue;

import com.dynxsty.dih4jda.interactions.components.ModalHandler;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.systems.qotw.commands.QOTWSubcommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Subcommand that allows staff-members to add question to the QOTW-Queue.
 */
@AutoDetectableComponentHandler("qotw-add-question")
public class AddQuestionSubcommand extends QOTWSubcommand implements ModalHandler {
	private final ExecutorService asyncPool;
	private final QuestionQueueRepository questionQueueRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param questionQueueRepository Dao class that represents the QOTW_QUESTION SQL Table.
	 * @param asyncPool The main thread pool for asynchronous operations
	 */
	public AddQuestionSubcommand(QuestionQueueRepository questionQueueRepository, ExecutorService asyncPool) {
		this.asyncPool = asyncPool;
		this.questionQueueRepository = questionQueueRepository;
		setSubcommandData(new SubcommandData("add", "Add a question to the queue."));
	}

	@Override
	protected InteractionCallbackAction<?> handleCommand(SlashCommandInteractionEvent event, long guildId) {
		return event.replyModal(buildQuestionModal());
	}

	private Modal buildQuestionModal() {
		TextInput priorityField = TextInput.create("priority", "Priority (Leave blank for default)", TextInputStyle.SHORT)
				.setRequired(false)
				.setValue("0")
				.build();

		TextInput questionField = TextInput.create("question", "Question Text", TextInputStyle.PARAGRAPH)
				.setMaxLength(1024)
				.build();

		return Modal.create("qotw-add-question", "Create QOTW Question")
				.addActionRows(ActionRow.of(questionField), ActionRow.of(priorityField))
				.build();
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, List<ModalMapping> values) {
			event.deferReply(true).queue();
			// Create question
			QOTWQuestion question = new QOTWQuestion();
			question.setGuildId(event.getGuild().getIdLong());
			question.setCreatedBy(event.getUser().getIdLong());
			question.setPriority(0);

			ModalMapping textOption = event.getValue("question");
			if (textOption == null || textOption.getAsString().isEmpty()) {
				Responses.warning(event.getHook(), "Invalid question text. Must not be blank, and must be less than 1024 characters.").queue();
				return;
			}
			question.setText(textOption.getAsString());

			ModalMapping priorityOption = event.getValue("priority");
			if (priorityOption == null || !priorityOption.getAsString().matches("\\d+")) {
				Responses.error(event.getHook(), "Invalid priority value. Must be a numeric value.").queue();
				return;
			}

			if (!priorityOption.getAsString().isEmpty()) {
				question.setPriority(Integer.parseInt(priorityOption.getAsString()));
			}

			asyncPool.submit(() -> {
				try {
					questionQueueRepository.save(question);
				} catch (DataAccessException e) {
					ExceptionLogger.capture(e, AddQuestionSubcommand.class.getSimpleName());
				}
			});
			Responses.success(event.getHook(), "Question Added", "Your question has been added to the queue.").queue();
		}
}
