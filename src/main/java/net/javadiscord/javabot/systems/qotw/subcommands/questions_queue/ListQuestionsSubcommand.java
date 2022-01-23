package net.javadiscord.javabot.systems.qotw.subcommands.questions_queue;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.subcommands.QOTWSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ListQuestionsSubcommand extends QOTWSubcommand {
	@Override
	protected ReplyAction handleCommand(SlashCommandEvent event, Connection con, long guildId) throws SQLException {
		var repository = new QuestionQueueRepository(con);
		OptionMapping pageOption = event.getOption("page");
		int page = 0;
		if (pageOption != null) {
			int userPage = (int) pageOption.getAsLong();
			if (userPage < 0) {
				return Responses.warning(event, "Invalid page.");
			}
			page = userPage;
		}

		var questions = repository.getQuestions(guildId, page, 10);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("QOTW Questions Queue");
		if (questions.isEmpty()) {
			embedBuilder.setDescription("There are no questions in the queue.");
			return event.replyEmbeds(embedBuilder.build());
		}
		Bot.asyncPool.submit(() -> {
			for (var question : questions) {
				embedBuilder.addField(
						String.valueOf(question.getId()),
						String.format(
								"> %s\nPriority: **%d**\nCreated by: %s\nCreated at: %s",
								question.getText(),
								question.getPriority(),
								event.getJDA().retrieveUserById(question.getCreatedBy()).complete().getAsTag(),
								question.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
						),
						false
				);
			}
			event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
		});
		return event.deferReply();
	}
}
