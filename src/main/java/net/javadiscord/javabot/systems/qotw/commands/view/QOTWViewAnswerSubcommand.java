package net.javadiscord.javabot.systems.qotw.commands.view;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.MessageActionUtils;
import net.javadiscord.javabot.util.Responses;

/**
 * Represents the `/qotw-view answer` subcommand. It allows for viewing an answer to a QOTW.
 */
public class QOTWViewAnswerSubcommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;
	private final ExecutorService asyncPool;
	private final QOTWSubmissionRepository qotwSubmissionRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param qotwSubmissionRepository Dao object that represents the QOTW_SUBMISSIONS SQL Table.
	 */
	public QOTWViewAnswerSubcommand(BotConfig botConfig, ExecutorService asyncPool, QOTWSubmissionRepository qotwSubmissionRepository) {
		this.botConfig = botConfig;
		this.asyncPool = asyncPool;
		this.qotwSubmissionRepository = qotwSubmissionRepository;
		setSubcommandData(new SubcommandData("answer", "Views the content of an answer to the Question of the Week")
				.addOption(OptionType.INTEGER, "question", "The question number the answer has been submitted to", true)
				.addOption(OptionType.USER, "answerer", "The user who answered the question", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			Responses.replyGuildOnly(event).setEphemeral(true).queue();
			return;
		}
		OptionMapping questionOption = event.getOption("question");
		if (questionOption == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		OptionMapping answerOwnerOption = event.getOption("answerer");
		if (answerOwnerOption == null) {
			Responses.error(event, "The answerer option is missing.").queue();
			return;
		}
		if (event.getChannelType() != ChannelType.TEXT) {
			Responses.error(event, "This command can only be used in text channels.").queue();
			return;
		}
		event.deferReply(true).queue();
		asyncPool.execute(()->{
			try {
				QOTWSubmission submission = qotwSubmissionRepository.getSubmissionByQuestionNumberAndAuthorID(event.getGuild().getIdLong(), questionOption.getAsInt(), answerOwnerOption.getAsUser().getIdLong());
				if (submission == null || !QOTWListAnswersSubcommand.isSubmissionVisible(submission, event.getUser().getIdLong())) {
					Responses.error(event.getHook(), "No answer to the question was found from the specific user.").queue();
					return;
				}
				botConfig.get(event.getGuild()).getQotwConfig()
						.getSubmissionChannel().retrieveArchivedPrivateThreadChannels().queue(threadChannels -> threadChannels
								.stream()
								.filter(c -> c.getIdLong() == submission.getThreadId())
								.findAny()
								.ifPresentOrElse(submissionChannel ->
												submissionChannel.getHistoryFromBeginning(100).queue(history ->
														MessageActionUtils.copyMessagesToNewThread(event.getGuildChannel().asStandardGuildMessageChannel(),
																buildQOTWInfoEmbed(submission, event.getMember() == null ? event.getUser().getName() : event.getMember().getEffectiveName()),
																"(qotw-view) Answer to QOTW #" + submission.getQuestionNumber(),
																history.getRetrievedHistory()
																	.stream()
																	.filter(msg -> !msg.getAuthor().isSystem() && !msg.getAuthor().isBot())
																	.sorted(Comparator.comparingLong(Message::getIdLong))
																	.toList(),
																thread -> {
																	Responses.success(event.getHook(), "View Answer", "Answer copied successfully").queue();
																	thread.getManager().setLocked(true).setArchived(true).queue();
																})),
										() -> Responses.error(event.getHook(), "The QOTW-Submission thread was not found.").queue()));
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, QOTWViewAnswerSubcommand.class.getSimpleName());
			}
		});
	}

	private @NotNull MessageEmbed buildQOTWInfoEmbed(@NotNull QOTWSubmission submission, String requester) {
		return new EmbedBuilder()
				.setTitle("Answer to Question of the Week #" + submission.getQuestionNumber())
				.setDescription("Answer by <@" + submission.getAuthorId() + ">")
				.setFooter("Requested by " + requester)
				.setColor(Responses.Type.DEFAULT.getColor())
				.build();
	}

}
