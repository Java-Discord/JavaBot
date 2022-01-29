package net.javadiscord.javabot.systems.qotw.submissions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;

import java.sql.SQLException;
import java.time.Instant;

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
	 * @param event          The {@link ButtonClickEvent} that is fired upon use.
	 * @param questionNumber The current qotw-week number.
	 * @return A {@link WebhookMessageAction}.
	 */
	public WebhookMessageAction<?> handleSubmission(ButtonClickEvent event, int questionNumber) {
		var member = event.getMember();
		if (!canCreateSubmissions(member)) {
			return Responses.warning(event.getHook(), "You're not eligible to create a new submission thread.");
		}
		config.getSubmissionChannel().createThreadChannel(
				String.format(THREAD_NAME, questionNumber, member.getEffectiveName()), true).queue(
				thread -> {
					try (var con = Bot.dataSource.getConnection()) {
						var repo = new QOTWSubmissionRepository(con);
						QOTWSubmission submission = new QOTWSubmission();
						submission.setThreadId(thread.getIdLong());
						submission.setQuestionNumber(questionNumber);
						submission.setGuildId(thread.getGuild().getIdLong());
						submission.setAuthorId(member.getIdLong());
						repo.insert(submission);

						thread.getManager().setInvitable(false).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue();
						thread.sendMessage(String.format("%s %s", config.getQOTWReviewRole(), member.getAsMention()))
								.setEmbeds(buildSubmissionThreadEmbed(event.getUser(), questionNumber, config))
								.setActionRows(ActionRow.of(Button.danger("qotw-submission:delete", "Delete Submission")))
								.queue();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}, e -> log.error("Could not create submission thread for member {}. ", member.getUser().getAsTag(), e)
		);
		log.info("Opened new Submission Thread for User {}", member.getUser().getAsTag());
		return Responses.success(event.getHook(), "Submission Thread created",
				"Successfully created a new private Thread for your submission.");

	}

	/**
	 * Handles the "Delete Submission" Button.
	 *
	 * @param event The {@link ButtonClickEvent} that is fired upon use.
	 */
	public void handleThreadDeletion(ButtonClickEvent event) {
		var thread = (ThreadChannel) event.getGuildChannel();
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QOTWSubmissionRepository(con);
			var submissionOptional = repo.getSubmissionByThreadId(thread.getIdLong());
			if (submissionOptional.isPresent()) {
				var submission = submissionOptional.get();
				if (submission.getAuthorId() != event.getMember().getIdLong()) return;
				repo.removeSubmission(thread.getIdLong());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new QOTWSubmissionRepository(con);
			return repo.getUnreviewedSubmissions(authorId).size() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private MessageEmbed buildSubmissionThreadEmbed(User createdBy, long questionNumber, QOTWConfig config) {
		return new EmbedBuilder()
				.setColor(Bot.config.get(config.getGuild()).getSlashCommand().getDefaultColor())
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle(String.format("Question of the Week #%s", questionNumber))
				.setDescription(String.format("Hey, %s! Please submit your answer into this thread." +
								"\nYou can even send multiple messages, if you want to. This whole thread counts as your submission." +
								"\nThe %s will review your submission once a new question appears.",
						createdBy.getAsMention(), config.getQOTWReviewRole().getAsMention()))
				.setTimestamp(Instant.now())
				.build();
	}
}
