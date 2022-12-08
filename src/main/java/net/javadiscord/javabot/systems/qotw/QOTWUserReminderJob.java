package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWSubmission;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;

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
	@Scheduled(cron = "* 0 16 * * 5")//Friday 16:00
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			QOTWConfig config = botConfig.get(guild).getQotwConfig();
			List<QOTWSubmission> submissions = new SubmissionManager(config, pointsService, questionQueueRepository, notificationService, asyncPool).getActiveSubmissions();
			for (QOTWSubmission submission : submissions) {
				UserPreference preference = userPreferenceService.getOrCreate(submission.getAuthor().getIdLong(), Preference.QOTW_REMINDER);
				if (Boolean.parseBoolean(preference.getState()) && submission.getThread().getMessageCount() <= 1) {
					submission.getThread()
							.sendMessageFormat("**Question of the Week Reminder**\nHey %s! You still have some time left to submit your answer!", submission.getAuthor().getAsMention())
							.queue();
				}
			}
		}
	}
}
