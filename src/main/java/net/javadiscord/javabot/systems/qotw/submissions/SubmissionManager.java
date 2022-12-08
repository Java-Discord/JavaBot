package net.javadiscord.javabot.systems.qotw.submissions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.systems.qotw.model.QOTWSubmission;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Handles & manages QOTW Submissions by using Discords {@link ThreadChannel}s.
 */
@Slf4j
@RequiredArgsConstructor
public class SubmissionManager {
	/**
	 * The submission thread's name.
	 */
	public static final String THREAD_NAME = "%s — %s";
	private static final String SUBMISSION_ACCEPTED = "\u2705";
	private static final String SUBMISSION_DECLINED = "\u274C";
	private static final String SUBMISSION_PENDING = "\uD83D\uDD52";
	private static final Map<Long, QOTWSubmission> submissionCache;

	static {
		submissionCache = new HashMap<>();
	}

	private final QOTWConfig config;
	private final QOTWPointsService pointsService;
	private final QuestionQueueRepository questionQueueRepository;
	private final NotificationService notificationService;
	private final ExecutorService asyncPool;

	/**
	 * Handles the "Submit your Answer" Button interaction.
	 *
	 * @param event          The {@link ButtonInteractionEvent} that is fired upon use.
	 * @param questionNumber The current qotw-week number.
	 * @return A {@link WebhookMessageCreateAction}.
	 */
	@Transactional
	public WebhookMessageCreateAction<?> handleSubmission(@NotNull ButtonInteractionEvent event, int questionNumber) {
		event.deferEdit().queue();
		Member member = event.getMember();
		if (!this.canCreateSubmissions(member)) {
			return Responses.warning(event.getHook(), "You're not eligible to create a new submission thread.");
		}
		config.getSubmissionChannel().createThreadChannel(
				String.format(THREAD_NAME, questionNumber, member.getEffectiveName()), true).queue(
				thread -> {
					thread.addThreadMember(member).queue();
					thread.getManager().setInvitable(false).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue();
					try {
						asyncPool.execute(() -> {
							Optional<QOTWQuestion> questionOptional = questionQueueRepository.findByQuestionNumber(questionNumber);
							if (questionOptional.isPresent()) {
								thread.sendMessage(member.getAsMention())
										.setEmbeds(buildSubmissionThreadEmbed(event.getUser(), questionOptional.get(), config))
										.setComponents(ActionRow.of(Button.danger("qotw-submission:delete", "Delete Submission")))
										.queue(s -> {
										}, err -> ExceptionLogger.capture(err, getClass().getSimpleName()));
								QOTWSubmission submission = new QOTWSubmission(thread);
								submission.setAuthor(member.getUser());
								submissionCache.put(thread.getIdLong(), submission);
							} else {
								thread.sendMessage("Could not retrieve current QOTW Question. Please contact an Administrator if you think that this is a mistake.")
										.queue();
							}
						});
					} catch (DataAccessException e) {
						ExceptionLogger.capture(e, getClass().getSimpleName());
					}
				}, e -> log.error("Could not create submission thread for member {}. ", member.getUser().getAsTag(), e)
		);
		log.info("Opened new Submission Thread for User {}", member.getUser().getAsTag());
		return Responses.success(event.getHook(), "Submission Thread created", "Successfully created a new private Thread for your submission.");
	}

	public List<QOTWSubmission> getActiveSubmissions() {
		return config.getSubmissionChannel().getThreadChannels()
				.stream()
				.map(this::getOrRetrieveSubmission).toList();
	}

	/**
	 * Handles the "Delete Submission" Button.
	 *
	 * @param event  The {@link ButtonInteractionEvent} that is fired upon use.
	 */
	public void handleThreadDeletion(@NotNull ButtonInteractionEvent event) {
		config.getSubmissionChannel().getThreadChannels()
				.stream().filter(t -> t.getIdLong() == event.getChannel().getIdLong())
				.map(this::getOrRetrieveSubmission)
				.forEach(s -> getOrRetrieveAuthor(s, author -> {
					if (event.getUser().getIdLong() == author.getIdLong()) {
						s.getThread().delete().queue();
					}
				}));
	}

	private void getOrRetrieveAuthor(@NotNull QOTWSubmission submission, Consumer<User> onSuccess) {
		if (submission.hasAuthor()) {
			onSuccess.accept(submission.getAuthor());
		} else {
			submission.retrieveAuthor(author -> {
				submission.setAuthor(author);
				submissionCache.put(submission.getThread().getIdLong(), submission);
				onSuccess.accept(author);
			});
		}
	}

	private QOTWSubmission getOrRetrieveSubmission(@NotNull ThreadChannel thread) {
		if (submissionCache.containsKey(thread.getIdLong())) {
			return submissionCache.get(thread.getIdLong());
		} else {
			QOTWSubmission submission = new QOTWSubmission(thread);
			submissionCache.put(thread.getIdLong(), submission);
			return submission;
		}
	}

	private boolean canCreateSubmissions(Member member) {
		if (member == null) return false;
		if (member.getUser().isBot() || member.getUser().isSystem()) return false;
		return !member.isTimedOut() && !member.isPending();
	}

	/**
	 * Accepts a submission.
	 *
	 * @param hook   The {@link net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 * @param author The submissions' author.
	 * @param bestAnswer Whether the submission is among the best answers for this week.
	 */
	public void acceptSubmission(InteractionHook hook, @NotNull ThreadChannel thread, @NotNull User author, boolean bestAnswer) {
		thread.getManager().setName(SUBMISSION_ACCEPTED + thread.getName().substring(1)).queue();
		pointsService.increment(author.getIdLong());
		notificationService.withQOTW(thread.getGuild(), author).sendAccountIncrementedNotification();
		Responses.success(hook, "Submission Accepted",
				"Successfully accepted submission by " + author.getAsMention()).queue();
		notificationService.withQOTW(thread.getGuild()).sendSubmissionActionNotification(author, getOrRetrieveSubmission(thread), bestAnswer ? SubmissionStatus.ACCEPT_BEST : SubmissionStatus.ACCEPT);
		// TODO: add forum handling
	}

	/**
	 * Declines a submission.
	 *
	 * @param hook   The {@link net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent} that was fired.
	 * @param thread The submission's {@link ThreadChannel}.
	 * @param author The submissions' author.
	 */
	public void declineSubmission(InteractionHook hook, @NotNull ThreadChannel thread, User author) {
		thread.getManager().setName(SUBMISSION_DECLINED + thread.getName().substring(1)).queue();
		// TODO: fix reason
		notificationService.withQOTW(thread.getGuild(), author).sendSubmissionDeclinedEmbed("EMPTY_REASON");
		Responses.success(hook, "Submission Declined", "Successfully declined submission by " + author.getAsMention()).queue();
		notificationService.withQOTW(thread.getGuild()).sendSubmissionActionNotification(author, getOrRetrieveSubmission(thread), SubmissionStatus.DECLINE);
	}

	private @NotNull List<Message> getSubmissionContent(@NotNull ThreadChannel thread) {
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

	private @NotNull MessageEmbed buildSubmissionThreadEmbed(@NotNull User createdBy, @NotNull QOTWQuestion question, @NotNull QOTWConfig config) {
		return new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle(String.format("Question of the Week #%s", question.getQuestionNumber()))
				.setDescription(String.format("""
								%s

								Hey, %s! Please submit your answer into this private thread.
								The %s will review your submission once a new question appears.""",
						question.getText(), createdBy.getAsMention(), config.getQOTWReviewRole().getAsMention()))
				.addField("Note",
						"""
								To maximize your chances of getting this week's QOTW Point make sure to:
								— Provide a **Code example** (if possible)
								— Try to answer the question as detailed as possible.

								Staff usually won't reply in here.""", false)
				.setTimestamp(Instant.now())
				.build();
	}
}
