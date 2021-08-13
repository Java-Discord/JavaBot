package com.javadiscord.javabot.qotw.subcommands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.qotw.dao.QuestionRepository;
import com.javadiscord.javabot.qotw.model.QOTWQuestion;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

public class AddQuestionSubcommand extends QOTWSubcommand {
	@Override
	protected ReplyAction handleCommand(SlashCommandEvent event, Connection con, long guildId) throws Exception {
		QOTWQuestion question = new QOTWQuestion();
		question.setGuildId(guildId);
		question.setCreatedBy(event.getUser().getIdLong());
		question.setPriority(0);

		OptionMapping textOption = event.getOption("question");
		if (textOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		String text = textOption.getAsString();
		if (text.isBlank() || text.length() > 1024) {
			return Responses.warning(event, "Invalid question text. Must not be blank, and must be less than 1024 characters.");
		}
		question.setText(text);

		OptionMapping priorityOption = event.getOption("priority");
		if (priorityOption != null) {
			question.setPriority((int) priorityOption.getAsLong());
		}

		new QuestionRepository(con).save(question);
		return Responses.success(event, "Question Added", "Your question has been added to the queue. Its id is `" + question.getId() + "`.");
	}
}
