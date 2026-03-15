package net.discordjug.javabot.systems.qotw.commands.questions_queue;

import net.discordjug.javabot.systems.qotw.commands.QOTWSubcommand;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWQuestion;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Subcommand that allows staff-members to list QOTW Questions.
 */
public class ListQuestionsSubcommand extends QOTWSubcommand {
	private final ExecutorService asyncPool;
	private final QuestionQueueRepository questionQueueRepository;
	
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param questionQueueRepository Dao class that represents the QOTW_QUESTION SQL Table.
	 * @param asyncPool The main thread pool for asynchronous operations
	 */
	public ListQuestionsSubcommand(QuestionQueueRepository questionQueueRepository, ExecutorService asyncPool) {
		this.asyncPool = asyncPool;
		this.questionQueueRepository = questionQueueRepository;
		setCommandData(new SubcommandData("list", "Show a list of all questions in the queue.")
				.addOption(OptionType.INTEGER, "page", "The page of results you get.", false)
		);
	}

	@Override
	protected InteractionCallbackAction<?> handleCommand(@NotNull SlashCommandInteractionEvent event, long guildId) throws DataAccessException {
		OptionMapping pageOption = event.getOption("page");
		int page = 0;
		if (pageOption != null) {
			int userPage = (int) pageOption.getAsLong();
			if (userPage < 0) {
				return Responses.warning(event, "Invalid page.");
			}
			page = userPage;
		}
		List<QOTWQuestion> questions = questionQueueRepository.getQuestions(guildId, page, 10);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(event.getUser()), null, event.getUser().getEffectiveAvatarUrl())
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
								UserSnowflake.fromId(question.getCreatedBy()).getAsMention(),
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
