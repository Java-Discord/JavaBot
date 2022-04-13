package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Removes a specified amount of experience from everyone's help account.
 */
public class HelpExperienceJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		DbHelper.doDaoAction(HelpAccountRepository::new, dao -> dao.removeExperienceFromAllAccounts(-1));
	}
}
