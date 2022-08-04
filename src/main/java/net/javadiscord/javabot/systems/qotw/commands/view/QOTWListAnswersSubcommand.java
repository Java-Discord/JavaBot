package net.javadiscord.javabot.systems.qotw.commands.view;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.systems.qotw.submissions.subcommands.MarkBestAnswerSubcommand;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the `/qotw-view list-answers` subcommand. It allows for listing answers to a specific QOTW.
 */
public class QOTWListAnswersSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public QOTWListAnswersSubcommand() {
		setSubcommandData(new SubcommandData("list-answers", "Lists answers to (previous) questions of the week")
				.addOption(OptionType.INTEGER, "question", "The question number", true)
		);
	}

	/**
	 * Checks whether a submission is visible to a specific user.
	 *
	 * @param submission the {@link QOTWSubmission} to be checked
	 * @param viewerId   the user to check against
	 * @return {@code true} if the submission is visible, else {@code false}}
	 */
	public static boolean isSubmissionVisible(@NotNull QOTWSubmission submission, long viewerId) {
		return submission.getStatus() == SubmissionStatus.ACCEPTED || submission.getAuthorId() == viewerId;
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		OptionMapping questionNumOption = event.getOption("question");
		if (questionNumOption == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		int questionNum = questionNumOption.getAsInt();

		event.deferReply(true).queue();
		DbActions.doAsyncDaoAction(QOTWSubmissionRepository::new, repo -> {
			List<QOTWSubmission> submissions = repo.getSubmissionsByQuestionNumber(event.getGuild().getIdLong(), questionNum);
			EmbedBuilder eb = new EmbedBuilder()
					.setTitle("Answers of Question of the Week #" + questionNum)
					.setColor(Responses.Type.DEFAULT.getColor())
					.setFooter("Results may not be accurate due to historic data.");
			TextChannel submissionChannel = Bot.getConfig().get(event.getGuild()).getQotwConfig().getSubmissionChannel();
			String allAnswers = submissions
					.stream()
					.filter(submission -> isSubmissionVisible(submission, event.getUser().getIdLong()))
					.filter(submission -> submission.getQuestionNumber() == questionNum)
					.map(s -> (isBestAnswer(submissionChannel,s) ?
							"**Best** " : s.getStatus() == SubmissionStatus.ACCEPTED ? "Accepted " : "") +
							"Answer by <@" + s.getAuthorId() + ">")
					.collect(Collectors.joining("\n"));
			if (allAnswers.isEmpty()) {
				allAnswers = "No accepted answers found.";
			}
			eb.appendDescription(allAnswers);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		});
	}

	private boolean isBestAnswer(TextChannel submissionChannel, QOTWSubmission submission) {
		return submissionChannel
				.retrieveArchivedPrivateThreadChannels()
				.stream()
				.filter(t -> t.getIdLong() == submission.getThreadId())
				.findAny()
				.map(MarkBestAnswerSubcommand::isSubmissionThreadABestAnswer)
				.orElse(false);
	}
}
