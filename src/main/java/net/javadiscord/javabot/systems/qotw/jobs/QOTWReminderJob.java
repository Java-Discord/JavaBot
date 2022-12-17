package net.javadiscord.javabot.systems.qotw.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Checks that there's a question in the QOTW queue ready for posting soon.
 */
@Service
@RequiredArgsConstructor
public class QOTWReminderJob {
	private final JDA jda;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final QuestionQueueRepository questionQueueRepository;

	/**
	 * Checks that there's a question in the QOTW queue ready for posting soon.
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 9 * * *") // Daily, 09:00 UTC
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			QOTWConfig config = botConfig.get(guild).getQotwConfig();
			Optional<QOTWQuestion> q = questionQueueRepository.getNextQuestion(guild.getIdLong());
			if (q.isEmpty()) {
				notificationService.withGuild(guild).sendToModerationLog(m -> m.sendMessageFormat(
						"Warning! %s There's no Question of the Week in the queue. Please add one before it's time to post!",
						config.getQOTWReviewRole().getAsMention()));
			}
		}
	}
}
