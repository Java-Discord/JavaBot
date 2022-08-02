package net.javadiscord.javabot.systems.qotw.submissions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
	private final QOTWConfig config;

	/**
	 * Handles the "Submit your Answer" Button interaction.
	 *
	 * @param event          The {@link ButtonInteractionEvent} that is fired upon use.
	 * @param questionNumber The current qotw-week number.
	 * @return A {@link WebhookMessageAction}.
	 */
	public WebhookMessageAction<?> handleSubmission(@NotNull ButtonInteractionEvent event, int questionNumber) {
		event.deferEdit().queue();
		Member member = event.getMember();
		if (!this.canCreateSubmissions(member)) {
			return Responses.warning(event.getHook(), "You're not eligible to create a new submission thread.");
		}
		config.getSubmissionChannel().createThreadChannel(
				String.format(THREAD_NAME, questionNumber, member.getEffectiveName()), true).queue(
				thread -> {
					thread.getManager().setInvitable(false).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue();
					try (Connection con = Bot.getDataSource().getConnection()) {
						QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
						QOTWSubmission submission = new QOTWSubmission();
						submission.setThreadId(thread.getIdLong());
						submission.setQuestionNumber(questionNumber);
						submission.setGuildId(thread.getGuild().getIdLong());
						submission.setAuthorId(member.getIdLong());
						repo.insert(submission);
						DbHelper.doDaoAction(QuestionQueueRepository::new, dao -> {
							Optional<QOTWQuestion> questionOptional = dao.findByQuestionNumber(questionNumber);
							if (questionOptional.isPresent()) {
								thread.sendMessage(member.getAsMention())
										.setEmbeds(buildSubmissionThreadEmbed(event.getUser(), questionOptional.get(), config))
										.setActionRows(ActionRow.of(Button.danger("qotw-submission:delete", "Delete Submission")))
										.queue();
							} else {
								thread.sendMessage("Could not retrieve current QOTW Question. Please contact an Administrator if you think that this is a mistake.")
										.queue();
							}
						});
					} catch (SQLException e) {
						ExceptionLogger.capture(e, getClass().getSimpleName());
					}
				}, e -> log.error("Could not create submission thread for member {}. ", member.getUser().getAsTag(), e)
		);
		log.info("Opened new Submission Thread for User {}", member.getUser().getAsTag());
		return Responses.success(event.getHook(), "Submission Thread created", "Successfully created a new private Thread for your submission.");

	}

	/**
	 * Handles the "Delete Submission" Button.
	 *
	 * @param event The {@link ButtonInteractionEvent} that is fired upon use.
	 */
	public void handleThreadDeletion(ButtonInteractionEvent event) {
		ThreadChannel thread = (ThreadChannel) event.getGuildChannel();
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> {
			Optional<QOTWSubmission> submissionOptional = dao.getSubmissionByThreadId(thread.getIdLong());
			if (submissionOptional.isPresent()) {
				QOTWSubmission submission = submissionOptional.get();
				if (submission.getAuthorId() != event.getMember().getIdLong()) {
					return;
				}
				dao.deleteSubmission(thread.getIdLong());
				thread.delete().queue();
			}
		});
	}

	private boolean canCreateSubmissions(Member member) {
		if (member == null) return false;
		if (member.getUser().isBot() || member.getUser().isSystem()) return false;
		if (member.isTimedOut() || member.isPending()) return false;
		return !hasActiveSubmissionThreads(member.getIdLong());
	}

	/**
	 * Checks if the given user has unreviewed submissions.
	 *
	 * @param authorId The user's id.
	 * @return Whether the user hat unreviewed submissions or not.
	 */
	public boolean hasActiveSubmissionThreads(long authorId) {
		try (Connection con = Bot.getDataSource().getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			return !repo.getUnreviewedSubmissions(authorId).isEmpty();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return false;
		}
	}

	/**
	 * Gets all active submission threads.
	 *
	 * @param guildId The ID of the guild to get the submission threads from
	 * @return An immutable {@link List} of {@link QOTWSubmission}s.
	 */
	public List<QOTWSubmission> getActiveSubmissionThreads(long guildId) {
		try (Connection con = Bot.getDataSource().getConnection()) {
			QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
			return repo.getSubmissionsByQuestionNumber(guildId, repo.getCurrentQuestionNumber());
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
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
