package net.javadiscord.javabot.systems.staff.expert_questions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.staff.expert_questions.ExpertSubcommand;
import net.javadiscord.javabot.systems.staff.expert_questions.dao.ExpertQuestionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Subcommand that allows staff-members to list expert questions in a random order.
 */
public class ListExpertQuestionsSubcommand extends ExpertSubcommand {
	@Override
	protected ReplyAction handleCommand(SlashCommandEvent event, Connection con) throws SQLException {
		var numberOption = event.getOption("amount");
		if (numberOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		var number = numberOption.getAsLong();
		var repo = new ExpertQuestionRepository(con);
		var questions = repo.getQuestions(event.getGuild().getIdLong());
		if (questions.size() < number || number < 0) {
			return Responses.error(event, "You may only choose an amount between 1 and " + questions.size());
		}
		Random r = new Random();
		var numbers = IntStream
				.generate(() -> r.nextInt(questions.size()))
				.distinct()
				.limit(number)
				.toArray();
		StringBuilder sb = new StringBuilder();
		for (var num : numbers) {
			var q = questions.get(num);
			sb.append(String.format("`%s`\n> ", q.getId())).append(q.getText()).append("\n\n");
		}
		var e = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setTitle(String.format("Questions (%s)", number))
				.setDescription(sb.toString())
				.build();
		return event.replyEmbeds(e);
	}
}
