package net.discordjug.javabot.systems.qotw.jobs;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.data.config.guild.QOTWConfig;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWQuestion;
import net.discordjug.javabot.systems.qotw.model.QOTWSubmission;
import net.discordjug.javabot.systems.qotw.submissions.SubmissionManager;
import net.discordjug.javabot.systems.qotw.submissions.SubmissionStatus;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ForumPostAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Job which disables the Submission button.
 */
@Service
@RequiredArgsConstructor
public class QOTWCloseSubmissionsJob {
	private static final String SUBMISSION_PENDING = "\uD83D\uDD52 ";

	private final JDA jda;
	private final QuestionQueueRepository questionQueueRepository;
	private final ExecutorService asyncPool;
	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final BotConfig botConfig;

	/**
	 * Disables the submission button.
	 *
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 17 * * 7") // Sunday, 17:00 UTC
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			// Disable 'Submit your Answer' button on latest QOTW
			GuildConfig config = botConfig.get(guild);
			QOTWConfig qotwConfig = config.getQotwConfig();
			qotwConfig.getSubmissionChannel().getManager()
					.putRolePermissionOverride(guild.getIdLong(), Collections.emptySet(), Set.of(Permission.MESSAGE_SEND_IN_THREADS,
							Permission.MESSAGE_SEND,
							Permission.CREATE_PRIVATE_THREADS,
							Permission.CREATE_PUBLIC_THREADS,
							Permission.MESSAGE_ADD_REACTION))
					.queue();
			TextChannel logChannel = config.getModerationConfig().getLogChannel();
			if (logChannel == null) continue;
			if (qotwConfig.getSubmissionChannel() == null || qotwConfig.getQuestionChannel() == null) continue;
			Message questionMessage = getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda);
			questionMessage.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:closed", "Submissions closed").asDisabled())).queue();
			if (qotwConfig.getSubmissionsForumChannel() == null) {
				sendReviewInformation(guild, qotwConfig, logChannel);
			} else {
				CompletableFuture.runAsync(() -> {
					
					MessageEmbed embed = questionMessage.getEmbeds().get(0);
					Optional<QOTWQuestion> questionOptional = questionQueueRepository.findByQuestionNumber(getQuestionNumberFromEmbed(embed));
					if (questionOptional.isPresent()) {
						QOTWQuestion question = questionOptional.get();
						try (MessageCreateData data = new MessageCreateBuilder()
								.setEmbeds(buildQuestionEmbed(question)).build()) {
							createPinnedAnswerForum(qotwConfig, question, data);
						}
					}
				}, asyncPool).whenComplete((success, ex) -> {
					if (ex != null) {
						ExceptionLogger.capture(ex, getClass().getName());
					}
					sendReviewInformation(guild, qotwConfig, logChannel);
				});
			}
		}
	}

	private void sendReviewInformation(Guild guild, QOTWConfig qotwConfig, TextChannel logChannel) {
		logChannel
			.sendMessageFormat("%s%nIt's review time! There are **%s** threads to review",
								qotwConfig.getQOTWReviewRole().getAsMention(),
								qotwConfig.getSubmissionChannel().getThreadChannels().size())
			.flatMap(msg -> msg.createThreadChannel("QOTW review"))
			.queue(thread -> {
				for (ThreadChannel submission : qotwConfig.getSubmissionChannel().getThreadChannels()) {
					submission.getManager().setName(SUBMISSION_PENDING + submission.getName()).queue();
					// remove the author
					final QOTWSubmission s = new QOTWSubmission(submission);
					s.retrieveAuthor(author -> {
						submission.removeThreadMember(author).queue();
						if (author.getIdLong() == qotwConfig.getQotwSampleAnswerUserId()) {
							SubmissionManager manager = new SubmissionManager(botConfig.get(guild).getQotwConfig(), pointsService, questionQueueRepository, notificationService, asyncPool);
							manager.copySampleAnswerSubmission(submission, author);
						} else {
							thread
								.sendMessage("%s by %s".formatted(submission.getAsMention(), author.getAsMention()))
								.addActionRow(buildSubmissionSelectMenu(jda, submission.getIdLong()))
								.queue();
						}
					});
					for (Member member : guild.getMembersWithRoles(qotwConfig.getQOTWReviewRole())) {
						thread.addThreadMember(member).queue();
					}
				}
			});
	}

	private void createPinnedAnswerForum(QOTWConfig qotwConfig, QOTWQuestion question, MessageCreateData data) {
		ForumPostAction createAnswerForumAction = qotwConfig.getSubmissionsForumChannel()
				.createForumPost(String.format("Week %s — %s", question.getQuestionNumber(), question.getText().replace("*", "")), data);
		if (qotwConfig.getSubmissionsForumOngoingReviewTag() != null) {
			createAnswerForumAction.setTags(qotwConfig.getSubmissionsForumOngoingReviewTag());
		}
		
		createAnswerForumAction.flatMap(f -> changeForumPin(f.getThreadChannel())).queue();
	}

	/**
	 * Removes the current pin in a forum channel and pins a new post.
	 * @param toPin the forum channel to pin
	 * @return a {@link RestAction} performing the pin change
	 */
	@CheckReturnValue
	private RestAction<Void> changeForumPin(ThreadChannel toPin) {
		ThreadChannelManager setPinned = toPin.getManager().setPinned(true);
		return toPin.getParentChannel()
				.getThreadChannels()
				.stream()
				.filter(ThreadChannel::isPinned)
				.findAny()
				.map(c->c.getManager().setPinned(false))
				.map(a -> a.flatMap(result -> setPinned))
				.orElse(setPinned);
	}

	private @Nonnull StringSelectMenu buildSubmissionSelectMenu(JDA jda, long threadId) {
		final SystemsConfig.EmojiConfig emojiConfig = botConfig.getSystems().getEmojiConfig();
		return StringSelectMenu.create(ComponentIdBuilder.build("qotw-submission-select", "review", threadId))
				.setPlaceholder("Select an action for this submission")
				.addOptions(
						SelectOption.of("Accept (Best Answer)", SubmissionStatus.ACCEPT_BEST.name())
								.withDescription("The submission is correct and is considered to be among the \"best answers\"")
								.withEmoji(emojiConfig.getSuccessEmote(jda)),
						SelectOption.of("Accept", SubmissionStatus.ACCEPT.name())
								.withDescription("The overall submission is correct")
								.withEmoji(emojiConfig.getSuccessEmote(jda)),
						SelectOption.of("Decline: Wrong Answer", SubmissionStatus.DECLINE_WRONG_ANSWER.name())
								.withDescription("The submission is simply wrong or falsely explained")
								.withEmoji(emojiConfig.getFailureEmote(jda)),
						SelectOption.of("Decline: Too short", SubmissionStatus.DECLINE_TOO_SHORT.name())
								.withDescription("The submission is way to short in comparison to other submissions")
								.withEmoji(emojiConfig.getFailureEmote(jda)),
						SelectOption.of("Decline: Empty Submission", SubmissionStatus.DECLINE_EMPTY.name())
								.withDescription("The submission was empty")
								.withEmoji(emojiConfig.getFailureEmote(jda)),
						SelectOption.of("Decline: Plagiarism/AI content", SubmissionStatus.DECLINE_PLAGIARISM.name())
								.withDescription("The submission is plagiarized and/or AI content")
								.withEmoji(emojiConfig.getFailureEmote(jda))
				).build();
	}

	private @NotNull MessageEmbed buildQuestionEmbed(@NotNull QOTWQuestion question) {
		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(question.getText())
				.build();
	}

	private int getQuestionNumberFromEmbed(@NotNull MessageEmbed embed) {
		return embed.getTitle() == null ? 0 : Integer.parseInt(embed.getTitle().replaceAll("\\D+", ""));
	}

	private Message getLatestQOTWMessage(@NotNull MessageChannel channel, QOTWConfig config, JDA jda) {
		MessageHistory history = channel.getHistory();
		Message message = null;
		while (message == null) {
			List<Message> messages = history.retrievePast(100).complete();
			for (Message m : messages) {
				if (m.getAuthor().equals(jda.getSelfUser()) && m.getContentRaw().equals(config.getQOTWRole().getAsMention())) {
					message = m;
					break;
				}
			}
		}
		return message;
	}
}
