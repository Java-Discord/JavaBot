package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.management.Notification;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Checks that there's a question in the QOTW queue ready for posting soon.
 */
public class QOTWReminderJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, @NotNull JDA jda) throws JobExecutionException {
		for (Guild guild : jda.getGuilds()) {
			ModerationConfig config = Bot.getConfig().get(guild).getModerationConfig();
			try (Connection c = Bot.getDataSource().getConnection()) {
				QuestionQueueRepository repo = new QuestionQueueRepository(c);
				Optional<QOTWQuestion> q = repo.getNextQuestion(guild.getIdLong());
				if (q.isEmpty()) {
					NotificationService.withGuild(guild).sendToModerationLog(m -> m.sendMessageFormat(
							"Warning! %s There's no Question of the Week in the queue. Please add one before it's time to post!",
							config.getStaffRole().getAsMention()));
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				NotificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageFormat(
						"Warning! %s Could not check to see if there's a question in the QOTW queue:\n```\n%s\n```\n",
						config.getStaffRole().getAsMention(), e.getMessage()));
				throw new JobExecutionException(e);
			}
		}
	}
}
