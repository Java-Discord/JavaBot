package net.javadiscord.javabot.systems.qotw.submissions.subcommands;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.jetbrains.annotations.NotNull;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.util.AutoCompleteUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.MessageActionUtils;
import net.javadiscord.javabot.util.Responses;

/**
 * <h3>This class represents the /qotw submissions mark-best command.</h3>
 * This Subcommand allows members of the QOTW Review Team to mark a single submission
 * as the "Best Answer" of the current Week, which then get sent back into the submission channel for
 * others to view.
 */
public class MarkBestAnswerSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public MarkBestAnswerSubcommand() {
		setSubcommandData(new SubcommandData("mark-best", "Marks a single QOTW Submission as on of the best answers.")
				.addOption(OptionType.STRING, "thread-id", "The submission's thread id.", true, true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("thread-id");
		if (idMapping == null || Checks.isInvalidLongInput(idMapping)) {
			Responses.error(event, "Please provide a valid thread id-").queue();
			return;
		}
		long threadId = Long.parseLong(idMapping.getAsString());
		GuildConfig config = Bot.getConfig().get(event.getGuild());
		ThreadChannel submissionThread = event.getGuild().getThreadChannelById(threadId);
		if (submissionThread == null) {
			Responses.error(event, "Could not find thread with id: `%s`", threadId).queue();
			return;
		}
		event.deferReply(true).queue();
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> {
			Optional<QOTWSubmission> submissionOptional = dao.getSubmissionByThreadId(threadId);
			if (submissionOptional.isEmpty()) {
				Responses.error(event.getHook(), "Could not find submission with thread id: `%s`", threadId).queue();
				return;
			}
			QOTWSubmission submission = submissionOptional.get();
			if (submission.getStatus() != SubmissionStatus.ACCEPTED) {
				Responses.error(event.getHook(), "The Submission must be reviewed and accepted!").queue();
				return;
			}
			if (isSubmissionThreadABestAnswer(submissionThread)) {
				Responses.error(event.getHook(), "The Submission was already marked as one of the best answers.").queue();
				return;
			}
			List<Message> messages = getSubmissionContent(submissionThread);
			event.getGuild().retrieveMemberById(submission.getAuthorId()).queue(
					member -> {
						if (member == null) {
							Responses.error(event.getHook(), "Could not find member with id: `%s`", submission.getAuthorId()).queue();
							return;
						}
						QOTWPointsService service = new QOTWPointsService(Bot.getDataSource());
						service.increment(member.getIdLong());
						NotificationService.withQOTW(event.getGuild(), member.getUser()).sendBestAnswerNotification();
						sendBestAnswer(event.getHook(), messages, member, submissionThread);
					}
			);
		});
	}

	/**
	 * Checks if the QOTW submission is marked as a best answer.
	 * @param submissionThread the thread were the submission was sent to
	 * @return {@code true} if it is a best answer, else {@code false}
	 */
	public static boolean isSubmissionThreadABestAnswer(ThreadChannel submissionThread) {
		return Bot.getConfig().get(submissionThread.getGuild()).getQotwConfig().getQuestionChannel().retrieveArchivedPublicThreadChannels().stream().anyMatch(thread -> thread.getName().equals(submissionThread.getName()));
	}

	private List<Message> getSubmissionContent(ThreadChannel thread) {
		List<Message> messages = new ArrayList<>();
		int count = thread.getMessageCount();
		while (count > 0) {
			List<Message> retrieved = thread.getHistory().retrievePast(Math.min(count, 100)).complete()
					.stream()
					.filter(m -> !m.getAuthor().isBot())
					.toList();
			messages.addAll(retrieved);
			count -= Math.min(count, 100);
		}
		Collections.reverse(messages);
		return messages;
	}

	private MessageEmbed buildBestAnswerEmbed(Member member) {
		return new EmbedBuilder()
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setTitle("Best Answer")
				.setColor(Responses.Type.DEFAULT.getColor())
				.setDescription(String.format("%s's submission was marked as one of the **best answers** (+1 QOTW-Point)", member.getAsMention()))
				.build();
	}

	/**
	 * Sends the {@link MarkBestAnswerSubcommand#buildBestAnswerEmbed} embeds into the Question Channel and creates a new
	 * Thread on it, which contains messages of the original submission.
	 *
	 * @param hook The {@link InteractionHook} that is used to respond to the Slash Command.
	 * @param messages The submission's messages.
	 * @param member The submission's author.
	 * @param submissionThread The submission's thread.
	 */
	private void sendBestAnswer(InteractionHook hook, List<Message> messages, Member member, ThreadChannel submissionThread) {
		MessageActionUtils.copyMessagesToNewThread(Bot.getConfig().get(member.getGuild()).getQotwConfig().getQuestionChannel(),
				this.buildBestAnswerEmbed(member),
				submissionThread.getName(),
				messages,
				thread -> Responses.success(hook, "Best Answer", "Successfully marked %s as the best answer", submissionThread.getAsMention()).queue());
	}

	/**
	 * Replies with all accepted Question of the Week Submissions..
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
	public static List<Command.Choice> replyAcceptedSubmissions(@NotNull CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Connection con = Bot.getDataSource().getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			QOTWConfig config = Bot.getConfig().get(event.getGuild()).getQotwConfig();
			List<QOTWSubmission> submissions = repo.getSubmissionsByQuestionNumber(event.getGuild().getIdLong(), repo.getCurrentQuestionNumber())
					.stream()
					.filter(submission -> submission.getStatus() == SubmissionStatus.ACCEPTED)
					.toList();
			submissions.forEach(submission -> {
				ThreadChannel thread = event.getGuild().getThreadChannelById(submission.getThreadId());
				String name = thread == null ? String.valueOf(submission.getThreadId()) : thread.getName();
				if (config.getQuestionChannel().getThreadChannels().stream().noneMatch(t -> t.getName().equals(name))) {
					choices.add(new Command.Choice(name, submission.getThreadId()));
				}
			});
		} catch (SQLException e) {
			ExceptionLogger.capture(e, MarkBestAnswerSubcommand.class.getSimpleName());
		}
		return AutoCompleteUtils.filterChoices(event, choices);
	}
}
