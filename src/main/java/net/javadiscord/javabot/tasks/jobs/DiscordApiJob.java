package net.javadiscord.javabot.tasks.jobs;

import net.dv8tion.jda.api.JDA;
import org.quartz.*;

import java.util.Map;

/**
 * A type of job which requires a reference to {@link net.dv8tion.jda.api.JDA}
 * to be available at execution time. Extend this class if your job needs the
 * api.
 */
public abstract class DiscordApiJob implements Job {
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		var jda = (JDA) context.getJobDetail().getJobDataMap().get("jda");
		execute(context, jda);
	}

	/**
	 * Executes the job using the provided context and discord API.
	 * @param context The job context.
	 * @param jda The Discord API.
	 * @throws JobExecutionException If an error occurs during the job.
	 */
	protected abstract void execute(JobExecutionContext context, JDA jda) throws JobExecutionException;

	/**
	 * Builder method that produces a {@link JobDetail} for the given job type,
	 * with job data initialized to include a reference to the given Discord API.
	 * @param jobType The type of job to create a job detail for.
	 * @param jda The Discord API.
	 * @return The created job detail.
	 */
	public static JobDetail build(Class<? extends DiscordApiJob> jobType, JDA jda) {
		return JobBuilder.newJob(jobType)
				.usingJobData(new JobDataMap(Map.of("jda", jda)))
				.build();
	}
}
