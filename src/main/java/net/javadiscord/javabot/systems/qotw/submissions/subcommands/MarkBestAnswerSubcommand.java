package net.javadiscord.javabot.systems.qotw.submissions.subcommands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Allows members of the QOTW Review Team to mark a single submission as the "Best Answer" of the current Week.
 */
public class MarkBestAnswerSubcommand implements SlashCommand {
	@Override
	public InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		OptionMapping threadIdOption = event.getOption("thread-id");
		if (threadIdOption == null) {
			return Responses.error(event, "Missing required Arguments.");
		}
		long threadId = Long.parseLong(threadIdOption.getAsString());
		GuildConfig config = Bot.config.get(event.getGuild());
		ThreadChannel submission = event.getGuild().getThreadChannelById(threadId);
		if (submission == null) {
			return Responses.error(event, String.format("Could not find thread with id: `%s`", threadId));
		}
		config.getQotw().getQuestionChannel().createThreadChannel(submission.getName()).queue(
				thread -> DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> {
					Optional<QOTWSubmission> sub = dao.getSubmissionByThreadId(thread.getIdLong());
					if (sub.isEmpty()) {
						event.getHook().sendMessageFormat("Could not find submission with thread id: `%s`", thread.getIdLong()).queue();
						return;
					}
					List<Message> messages = this.getSubmissionContent(submission);
					messages.forEach(m -> thread.sendMessage(m.getContentRaw()).queue());
					event.getHook().sendMessageFormat("Successfully marked %s as the best answer", submission.getAsMention()).queue();
				})
		);
		return event.deferReply();
	}

	private List<Message> getSubmissionContent(ThreadChannel thread) {
		List<Message> messages = new ArrayList<>();
		int count = thread.getMessageCount();
		while (count > 0) {
			List<Message> retrieved = thread.getHistory().retrievePast(Math.min(count, 100)).complete();
			messages.addAll(retrieved);
			count -= Math.min(count, 100);
		}
		Collections.reverse(messages);
		return messages;
	}

	/**
	 * Replies with all accepted Question of the Week Submissions..
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
	public static List<Command.Choice> replyAcceptedSubmissions(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.dataSource.getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			List<QOTWSubmission> submissions = repo.getSubmissionByQuestionNumber(repo.getCurrentQuestionNumber());
			submissions.forEach(submission -> {
				ThreadChannel thread = event.getGuild().getThreadChannelById(submission.getThreadId());
				String name = thread == null ? String.valueOf(submission.getThreadId()) : thread.getName();
				choices.add(new Command.Choice(name, submission.getThreadId()));
			});
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return choices;
	}
}
