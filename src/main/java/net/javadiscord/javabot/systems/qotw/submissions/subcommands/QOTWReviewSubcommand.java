package net.javadiscord.javabot.systems.qotw.submissions.subcommands;

import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.User;
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
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

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
		submissionThread.retrieveThreadMembers().queue(
				members -> {
					Optional<ThreadMember> authorOptional = members.stream()
							.filter(m -> !m.getUser().isBot())
							.findFirst();
					if (authorOptional.isEmpty()) {
						Responses.info(event.getHook(), "Could not find submission author of thread %s", submissionThread.getAsMention()).queue();
						return;
					}
					User author = authorOptional.get().getUser();
					SubmissionManager manager = new SubmissionManager(qotwConfig, pointsService, questionQueueRepository, notificationService, asyncPool);
					if (state.contains("ACCEPT")) {
						manager.acceptSubmission(event.getHook(), submissionThread, author, state.equals("ACCEPT_BEST"));
					} else {
						manager.declineSubmission(event.getHook(), submissionThread, author);
					}
				}
		);
	}
}
