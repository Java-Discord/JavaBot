package net.javadiscord.javabot.systems.notification;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Sends Notifications regarding QOTW.
 */
@Slf4j
public final class QOTWNotificationService extends NotificationService {

	@Nullable
	private final User user;
	private final Guild guild;
	private final QOTWAccount account;

	/**
	 * The constructor of this class.
	 *
	 * @param user  The user that should be notified.
	 * @param guild The guild from where the notification was sent.
	 */
	public QOTWNotificationService(User user, Guild guild) {
		this.user = user;
		this.guild = guild;
		QOTWAccount account;
		try (Connection connection = Bot.dataSource.getConnection()) {
			QuestionPointsRepository dao = new QuestionPointsRepository(connection);
			account = dao.getByUserId(user.getIdLong());
		} catch (SQLException e) {
			log.error("Could not find Account with user Id: {}", user.getIdLong(), e);
			account = null;
		}
		this.account = account;
	}

	/**
	 * The constructor of this class.
	 *
	 * @param guild The guild from where the notification was sent.
	 */
	public QOTWNotificationService(Guild guild) {
		this.user = null;
		this.guild = guild;
		this.account = null;
	}

	public void sendBestAnswerNotification() {
		if (user == null || account == null) throw new UnsupportedOperationException("Can't send private messages with a guild-only constructor!");
		this.sendDirectMessageNotification(user, this.buildBestAnswerEmbed(account.getPoints()));
	}

	public void sendAccountIncrementedNotification() {
		if (user == null || account == null) throw new UnsupportedOperationException("Can't send private messages with a guild-only constructor!");
		this.sendDirectMessageNotification(user, this.buildAccountIncrementEmbed(account.getPoints()));
	}

	public void sendSubmissionDeclinedEmbed(@Nonnull String reason) {
		if (user == null || account == null) throw new UnsupportedOperationException("Can't send private messages with a guild-only constructor!");
		this.sendDirectMessageNotification(user, this.buildSubmissionDeclinedEmbed(reason));
	}

	/**
	 * Sends the executed action, performed on a QOTW submission thread, to the {@link Guild}s log channel.
	 *
	 * @param reviewedBy The user which reviewed the QOTW submission thread.
	 * @param submissionThread The submission thread itself.
	 * @param status The {@link SubmissionStatus}.
	 * @param reasons The reasons for taking this action.
	 */
	public void sendSubmissionActionNotification(User reviewedBy, ThreadChannel submissionThread, SubmissionStatus status, @Nullable String... reasons) {
		DbHelper.doDaoAction(QOTWSubmissionRepository::new, dao -> {
			Optional<QOTWSubmission> submissionOptional = dao.getSubmissionByThreadId(submissionThread.getIdLong());
			submissionOptional.ifPresent(submission -> guild.getJDA().retrieveUserById(submission.getAuthorId()).queue(author -> {
				new GuildNotificationService(guild).sendLogChannelNotification(this.buildSubmissionActionEmbed(author, submissionThread, reviewedBy, status, reasons));
				log.info("{} {} {}'s QOTW Submission{}", reviewedBy.getAsTag(), status.name().toLowerCase(), author.getAsTag(), reasons != null ? " for: " + String.join(", ", reasons) : ".");
			}));
		});
	}

	private EmbedBuilder buildQOTWNotificationEmbed() {
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Notification")
				.setTimestamp(Instant.now());
	}

	private MessageEmbed buildBestAnswerEmbed(long points) {
		return this.buildQOTWNotificationEmbed()
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was marked as the best answer!
								You've been granted **`1 extra QOTW-Point`**! (total: %s)""", points))
				.build();
	}

	private MessageEmbed buildAccountIncrementEmbed(long points) {
		return this.buildQOTWNotificationEmbed()
				.setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
				.setDescription(String.format(
						"""
								Your submission was accepted! %s
								You've been granted **`1 QOTW-Point`**! (total: %s)""",
						Bot.config.get(guild).getEmote().getSuccessEmote().getAsMention(), points))
				.build();
	}

	private MessageEmbed buildSubmissionDeclinedEmbed(String reasons) {
		return this.buildQOTWNotificationEmbed()
				.setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
				.setDescription(String.format("""
								Hey %s,
								Your QOTW-Submission was **declined** for the following reasons:
								**`%s`**

								However, you can try your luck again next week!""",
						user.getAsMention(), reasons))
				.build();
	}

	private MessageEmbed buildSubmissionActionEmbed(User author, ThreadChannel thread, User reviewedBy, SubmissionStatus status, String... reasons) {
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(reviewedBy.getAsTag(), null, reviewedBy.getEffectiveAvatarUrl())
				.setTitle(String.format("%s %s %s's QOTW Submission", reviewedBy.getAsTag(), status.name().toLowerCase(), author.getAsTag()))
				.setTimestamp(Instant.now());
		if (thread != null && status != SubmissionStatus.DELETED) {
			builder.addField("Thread", thread.getAsMention(), true);
		}
		if (reasons != null && reasons.length > 0) {
			builder.addField("Reason(s)", String.join(", ", reasons), true);
		}
		return builder.build();
	}
}
