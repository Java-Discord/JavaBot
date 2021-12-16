package com.javadiscord.javabot.service.qotw;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.service.qotw.dao.QuestionRepository;
import com.javadiscord.javabot.service.schedule.jobs.DiscordApiJob;
import com.javadiscord.javabot.utils.Misc;
import net.dv8tion.jda.api.JDA;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.SQLException;

/**
 * Checks that there's a question in the QOTW queue ready for posting soon.
 */
public class QOTWReminderJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		for (var guild : jda.getGuilds()) {
			try (var c = Bot.dataSource.getConnection()) {
				var repo = new QuestionRepository(c);
				var q = repo.getNextQuestion(guild.getIdLong());
				if (q.isEmpty()) {
					Misc.sendToLogFormat(guild, "Warning! @here There's no Question of the Week in the queue. Please add one before it's time to post!");
				}
			} catch (SQLException e) {
				e.printStackTrace();
				Misc.sendToLogFormat(guild, "Warning! @here Could not check to see if there's a question in the QOTW queue:\n```\n%s\n```\n", e.getMessage());
				throw new JobExecutionException(e);
			}
		}
	}
}
