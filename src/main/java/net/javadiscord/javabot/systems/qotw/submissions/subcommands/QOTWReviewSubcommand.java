package net.javadiscord.javabot.systems.qotw.submissions.subcommands;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWSubmission;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * <h3>This class represents the /qotw submissions review command.</h3>
 */
public class QOTWReviewSubcommand extends SlashCommand.Subcommand {

	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final QuestionQueueRepository questionQueueRepository;
	private final BotConfig botConfig;
	private final ExecutorService asyncPool;


	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param pointsService the {@link QOTWPointsService}
	 * @param notificationService The {@link NotificationService}
	 * @param questionQueueRepository The {@link QuestionQueueRepository}.
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 */
	public QOTWReviewSubcommand(QOTWPointsService pointsService, NotificationService notificationService, QuestionQueueRepository questionQueueRepository, BotConfig botConfig, ExecutorService asyncPool) {
		this.pointsService = pointsService;
		this.notificationService = notificationService;
		this.questionQueueRepository = questionQueueRepository;
		this.botConfig = botConfig;
		this.asyncPool = asyncPool;
		setCommandData(new SubcommandData("review", "Administrative command for reviewing QOTW-submissions")
				.addOptions(
						new OptionData(OptionType.CHANNEL, "submission", "A users' submission", true)
								.setChannelTypes(ChannelType.GUILD_PRIVATE_THREAD),
						new OptionData(OptionType.STRING, "state", "The submissions state", true)
								.addChoice("Accepted (Best Answer)", "ACCEPT_BEST")
								.addChoice("Accepted", "ACCEPT")
								.addChoice("Decline", "DECLINE")
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping submissionThreadMapping = event.getOption("submission");
		OptionMapping stateMapping = event.getOption("state");
		if (submissionThreadMapping == null || stateMapping == null || !submissionThreadMapping.getChannelType().isThread()) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		QOTWConfig qotwConfig = botConfig.get(event.getGuild()).getQotwConfig();
		ThreadChannel submissionThread = submissionThreadMapping.getAsChannel().asThreadChannel();
		String state = stateMapping.getAsString();
		if (submissionThread.getParentChannel().getIdLong() != qotwConfig.getSubmissionChannelId()) {
			Responses.error(event, "The selected thread is not a submission channel!").queue();
			return;
		}
		event.deferReply().queue();
		QOTWSubmission submission = new QOTWSubmission(submissionThread);
		submission.retrieveAuthor(author -> {
			SubmissionManager manager = new SubmissionManager(qotwConfig, pointsService, questionQueueRepository, notificationService, asyncPool);
			if (state.contains("ACCEPT")) {
				manager.acceptSubmission(submissionThread, author, event.getMember(), state.equals("ACCEPT_BEST"));
				Responses.success(event.getHook(), "Submission Accepted", "Successfully accepted submission by " + author.getAsMention()).queue();
			} else {
				// just do a "wrong answer" for now. this command is going to be removed
				// in the near future anyway
				manager.declineSubmission(submissionThread, author, event.getMember(), SubmissionStatus.DECLINE_WRONG_ANSWER);
				Responses.success(event.getHook(), "Submission Declined", "Successfully declined submission by " + author.getAsMention()).queue();
			}
			if (qotwConfig.getSubmissionChannel().getThreadChannels().size() <= 1) {
				Optional<ThreadChannel> newestPostOptional = qotwConfig.getSubmissionsForumChannel().getThreadChannels()
						.stream().max(Comparator.comparing(ThreadChannel::getTimeCreated));
				newestPostOptional.ifPresent(p -> {
					p.getManager().setAppliedTags().queue();
					notificationService.withGuild(qotwConfig.getGuild()).sendToModerationLog(log -> log.sendMessageFormat("All submissions have been reviewed!"));
				});
			}
		});
	}
}
