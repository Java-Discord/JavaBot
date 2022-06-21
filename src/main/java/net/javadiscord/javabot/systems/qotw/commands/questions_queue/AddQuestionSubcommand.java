package net.javadiscord.javabot.systems.qotw.commands.questions_queue;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.commands.QOTWSubcommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.Responses;

import java.sql.Connection;
import java.util.List;

/**
 * Subcommand that allows staff-members to add question to the QOTW-Queue.
 */
public class AddQuestionSubcommand extends QOTWSubcommand {
	public AddQuestionSubcommand() {
		setSubcommandData(new SubcommandData("add", "Add a question to the queue."));
		handleModalIds("qotw-add-question");
	}

	@Override
	protected InteractionCallbackAction<?> handleCommand(SlashCommandInteractionEvent event, Connection con, long guildId) {
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
	public void handleModal(ModalInteractionEvent event, List<ModalMapping> values) {
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

			DbHelper.doDaoAction(QuestionQueueRepository::new, dao -> dao.save(question));
			Responses.success(event.getHook(), "Question Added", "Your question has been added to the queue.").queue();
		}
}
