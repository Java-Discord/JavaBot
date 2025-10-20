package net.discordjug.javabot.systems.qotw.commands.questions_queue;

import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.systems.qotw.commands.QOTWSubcommand;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWQuestion;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;

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
		setCommandData(new SubcommandData("add", "Add a question to the queue."));
	}

	@Override
	protected InteractionCallbackAction<?> handleCommand(@NotNull SlashCommandInteractionEvent event, long guildId) {
		return event.replyModal(buildQuestionModal());
	}

	private @NotNull Modal buildQuestionModal() {
		TextInput priorityField = TextInput.create("priority", TextInputStyle.SHORT)
				.setRequired(false)
				.setValue("0")
				.build();
		TextInput questionField = TextInput.create("question", TextInputStyle.PARAGRAPH)
				.setMaxLength(85)
				.build();

		return Modal.create("qotw-add-question", "Create QOTW Question")
				.addComponents(Label.of("Question Text", questionField), Label.of("Priority (Leave blank for default)", priorityField))
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
