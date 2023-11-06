package net.discordjug.javabot.systems.qotw.jobs;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.QOTWConfig;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWSubmission;
import net.discordjug.javabot.systems.qotw.submissions.SubmissionManager;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.discordjug.javabot.systems.user_preferences.model.Preference;
import net.discordjug.javabot.systems.user_preferences.model.UserPreference;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Checks that there's a question in the QOTW queue ready for posting soon.
 */
@Service
@RequiredArgsConstructor
public class QOTWUserReminderJob {
	private final JDA jda;
	private final UserPreferenceService userPreferenceService;
	private final BotConfig botConfig;
	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;
	private final QuestionQueueRepository questionQueueRepository;
	private final ExecutorService asyncPool;

	/**
	 * Checks that there's a question in the QOTW queue ready for posting soon.
	 */
	@Scheduled(cron = "0 0 16 * * 5") // Friday, 16:00 UTC
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			QOTWConfig config = botConfig.get(guild).getQotwConfig();
			List<QOTWSubmission> submissions = new SubmissionManager(config, pointsService, questionQueueRepository, notificationService, asyncPool).getActiveSubmissions();
			for (QOTWSubmission submission : submissions) {
				submission.retrieveAuthor(author -> {
					UserPreference preference = userPreferenceService.getOrCreate(author.getIdLong(), Preference.QOTW_REMINDER);
					if (Boolean.parseBoolean(preference.getState()) && submission.getThread().getMessageCount() <= 2) {
						submission.getThread()
								.sendMessageFormat("**Question of the Week Reminder**\nHey %s! You still have some time left to submit your answer!", submission.getAuthor().getAsMention())
								.queue();
					}
				});

			}
		}
	}
}
