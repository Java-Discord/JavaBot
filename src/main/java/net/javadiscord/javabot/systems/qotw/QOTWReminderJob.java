package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * Checks that there's a question in the QOTW queue ready for posting soon.
 */
@Service
@RequiredArgsConstructor
public class QOTWReminderJob {
	private final JDA jda;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final DataSource dataSource;

	/**
	 * Checks that there's a question in the QOTW queue ready for posting soon.
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 9 * * *")//daily, 09:00
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			ModerationConfig config = botConfig.get(guild).getModerationConfig();
			try (Connection c = dataSource.getConnection()) {
				QuestionQueueRepository repo = new QuestionQueueRepository(c);
				Optional<QOTWQuestion> q = repo.getNextQuestion(guild.getIdLong());
				if (q.isEmpty()) {
					notificationService.withGuild(guild).sendToModerationLog(m -> m.sendMessageFormat(
							"Warning! %s There's no Question of the Week in the queue. Please add one before it's time to post!",
							config.getStaffRole().getAsMention()));
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				notificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageFormat(
						"Warning! %s Could not check to see if there's a question in the QOTW queue:\n```\n%s\n```\n",
						config.getStaffRole().getAsMention(), e.getMessage()));
				throw e;
			}
		}
	}
}
