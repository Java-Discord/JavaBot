package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
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
			ModerationConfig config = Bot.config.get(guild).getModeration();
			try (var c = Bot.dataSource.getConnection()) {
				var repo = new QuestionQueueRepository(c);
				var q = repo.getNextQuestion(guild.getIdLong());
				if (q.isEmpty()) {
					GuildUtils.getLogChannel(guild).sendMessageFormat(
							"Warning! %s There's no Question of the Week in the queue. Please add one before it's time to post!",
							config.getStaffRole().getAsMention()).queue();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				GuildUtils.getLogChannel(guild).sendMessageFormat(
						"Warning! %s Could not check to see if there's a question in the QOTW queue:\n```\n%s\n```\n",
						config.getStaffRole().getAsMention(), e.getMessage()).queue();
				throw new JobExecutionException(e);
			}
		}
	}
}
