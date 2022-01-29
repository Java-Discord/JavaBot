package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import net.javadiscord.javabot.util.GuildUtils;
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
				var repo = new QuestionQueueRepository(c);
				var q = repo.getNextQuestion(guild.getIdLong());
				if (q.isEmpty()) {
					GuildUtils.getLogChannel(guild).sendMessageFormat("Warning! @here There's no Question of the Week in the queue. Please add one before it's time to post!").queue();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				GuildUtils.getLogChannel(guild).sendMessageFormat("Warning! @here Could not check to see if there's a question in the QOTW queue:\n```\n%s\n```\n", e.getMessage()).queue();
				throw new JobExecutionException(e);
			}
		}
	}
}
