package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionControlsManager;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Job which disables the Submission button.
 */
@Service
@RequiredArgsConstructor
public class QOTWCloseSubmissionsJob {
	private final JDA jda;
	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final DbHelper dbHelper;
	private final QOTWSubmissionRepository qotwSubmissionRepository;

	/**
	 * disable the Submission button.
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 21 * * 7")//Sunday 21:00
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			// Disable 'Submit your Answer' button on latest QOTW
			GuildConfig config = botConfig.get(guild);
			QOTWConfig qotwConfig = config.getQotwConfig();
			qotwConfig.getSubmissionChannel().getManager()
					.putRolePermissionOverride(guild.getIdLong(), Collections.emptySet(), Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS))
					.queue();
			if (config.getModerationConfig().getLogChannel() == null) continue;
			if (qotwConfig.getSubmissionChannel() == null || qotwConfig.getQuestionChannel() == null) continue;
			Message message = getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda);
			if (message == null) continue;
			message.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:closed", "Submissions closed").asDisabled())).queue();
			for (ThreadChannel thread : qotwConfig.getSubmissionChannel().getThreadChannels()) {
				try (Connection con = dbHelper.getDataSource().getConnection()) {
					Optional<QOTWSubmission> optionalSubmission = qotwSubmissionRepository.getSubmissionByThreadId(thread.getIdLong());
					if (optionalSubmission.isEmpty()) continue;
					new SubmissionControlsManager(botConfig.get(guild), optionalSubmission.get(), pointsService, notificationService).sendControls();
				} catch (SQLException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					throw e;
				}
			}
		}
	}

	private Message getLatestQOTWMessage(MessageChannel channel, QOTWConfig config, JDA jda) {
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
