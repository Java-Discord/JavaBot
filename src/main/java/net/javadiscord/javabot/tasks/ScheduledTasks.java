package net.javadiscord.javabot.tasks;

import net.dv8tion.jda.api.JDA;
import net.javadiscord.javabot.systems.help.HelpExperienceJob;
import net.javadiscord.javabot.systems.qotw.QOTWCloseSubmissionsJob;
import net.javadiscord.javabot.systems.qotw.QOTWJob;
import net.javadiscord.javabot.systems.qotw.QOTWReminderJob;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * This class is responsible for setting up all scheduled tasks that the bot
 * should run periodically, using the Quartz {@link Scheduler}. To add new tasks
 * to the schedule, add them to the {@link ScheduledTasks#scheduleAllTasks(Scheduler, net.dv8tion.jda.api.JDA)}
 * method.
 */
public class ScheduledTasks {
	// Hide the constructor.
	private ScheduledTasks() {
	}

	/**
	 * Initializes all scheduled jobs and starts the scheduler. Also adds a
	 * shutdown hook that gracefully stops the scheduler when the program ends.
	 *
	 * @param jda The Discord API, which may be needed by some jobs.
	 * @throws SchedulerException If an error occurs while starting the scheduler.
	 */
	public static void init(JDA jda) throws SchedulerException {
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduleAllTasks(scheduler, jda);
		scheduler.start();
		// Add a hook to shut down the scheduler cleanly when the program terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				scheduler.shutdown();
			} catch (SchedulerException e) {
				ExceptionLogger.capture(e, ScheduledTasks.class.getSimpleName());
			}
		}));
	}

	/**
	 * This method is where all tasks are scheduled. <strong>Add scheduled tasks
	 * to the scheduler using this method!</strong>
	 *
	 * @param scheduler The scheduler to use.
	 * @param jda       The Discord API.
	 * @throws SchedulerException If an error occurs while adding a task.
	 */
	private static void scheduleAllTasks(Scheduler scheduler, JDA jda) throws SchedulerException {
		// Schedule posting a new QOTW every Monday at 9am.
		scheduleApiJob(scheduler, jda, QOTWJob.class, CronScheduleBuilder.weeklyOnDayAndHourAndMinute(DateBuilder.MONDAY, 9, 0));

		// Schedule closing submissions every Sunday at 9pm.
		scheduleApiJob(scheduler, jda, QOTWCloseSubmissionsJob.class, CronScheduleBuilder.weeklyOnDayAndHourAndMinute(DateBuilder.SUNDAY, 21, 0));

		// Schedule checking to make sure there's a new QOTW question in the queue.
		// We schedule this to run daily at 9am, just so we're always aware when the QOTW queue goes empty.
		scheduleApiJob(scheduler, jda, QOTWReminderJob.class, CronScheduleBuilder.dailyAtHourAndMinute(9, 0));

		// Schedule daily experience subtraction
		scheduleApiJob(scheduler, jda, HelpExperienceJob.class, CronScheduleBuilder.dailyAtHourAndMinute(0, 0));
	}

	/**
	 * Convenience method for scheduling an API-dependent job using a single
	 * trigger that follows a given schedule.
	 *
	 * @param scheduler       The scheduler to add the job to.
	 * @param jda             The Discord API.
	 * @param type            The type of job to schedule.
	 * @param scheduleBuilder A schedule builder that the trigger will use.
	 * @throws SchedulerException If an error occurs while adding the job.
	 * @see SimpleScheduleBuilder
	 * @see CronScheduleBuilder
	 * @see CalendarIntervalScheduleBuilder
	 */
	private static void scheduleApiJob(
			Scheduler scheduler,
			JDA jda,
			Class<? extends DiscordApiJob> type,
			ScheduleBuilder<?> scheduleBuilder
	) throws SchedulerException {
		scheduler.scheduleJob(
				DiscordApiJob.build(type, jda),
				TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).build()
		);
	}
}
