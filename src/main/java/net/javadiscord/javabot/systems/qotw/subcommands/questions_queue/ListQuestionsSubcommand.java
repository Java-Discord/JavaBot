package net.javadiscord.javabot.systems.qotw.subcommands.questions_queue;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.systems.qotw.subcommands.QOTWSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneOffset;

/**
 * Subcommand that allows staff-members to list QOTW Questions.
 */
public class ListQuestionsSubcommand extends QOTWSubcommand {
	@Override
	protected InteractionCallbackAction<?> handleCommand(SlashCommandInteractionEvent event, Connection con, long guildId) throws SQLException {
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
				.setAuthor(event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
				.setTitle("QOTW Questions Queue")
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor());
		if (questions.isEmpty()) {
			embedBuilder.setDescription("There are no questions in the queue.");
			return event.replyEmbeds(embedBuilder.build());
		}
		Bot.asyncPool.submit(() -> {
			for (QOTWQuestion question : questions) {
				embedBuilder.addField(
						String.valueOf(question.getId()),
						String.format(
								"> %s\nPriority: **%d**\nCreated by: %s\nCreated at: <t:%s:D>",
								question.getText(),
								question.getPriority(),
								event.getJDA().retrieveUserById(question.getCreatedBy()).complete().getAsMention(),
								question.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
						),
						false
				);
			}
			event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
		});
		return event.deferReply();
	}
}
