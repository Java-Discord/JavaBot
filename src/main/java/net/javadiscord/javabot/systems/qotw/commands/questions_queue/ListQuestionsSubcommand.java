package net.javadiscord.javabot.systems.qotw.commands.questions_queue;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.systems.qotw.commands.QOTWSubcommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

/**
 * Subcommand that allows staff-members to list QOTW Questions.
 */
public class ListQuestionsSubcommand extends QOTWSubcommand {
	private ExecutorService asyncPool;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param dataSource A factory for connections to the main database
	 */
	public ListQuestionsSubcommand(DataSource dataSource) {
		super(dataSource);
		setSubcommandData(new SubcommandData("list", "Show a list of all questions in the queue.")
				.addOption(OptionType.INTEGER, "page", "The page of results you get.", false)
		);
	}

	@Override
	protected InteractionCallbackAction<?> handleCommand(@NotNull SlashCommandInteractionEvent event, Connection con, long guildId) throws SQLException {
		QuestionQueueRepository repository = new QuestionQueueRepository(con);
		OptionMapping pageOption = event.getOption("page");
		int page = 0;
		if (pageOption != null) {
			int userPage = (int) pageOption.getAsLong();
			if (userPage < 0) {
				return Responses.warning(event, "Invalid page.");
			}
			page = userPage;
		}
		List<QOTWQuestion> questions = repository.getQuestions(guildId, page, 10);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setAuthor(event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
				.setTitle("QOTW Questions Queue")
				.setColor(Responses.Type.DEFAULT.getColor());
		if (questions.isEmpty()) {
			embedBuilder.setDescription("There are no questions in the queue.");
			return event.replyEmbeds(embedBuilder.build());
		}
		asyncPool.submit(() -> {
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
