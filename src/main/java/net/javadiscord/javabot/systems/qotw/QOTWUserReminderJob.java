package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Checks that there's a question in the QOTW queue ready for posting soon.
 */
@Service
@RequiredArgsConstructor
public class QOTWUserReminderJob {
	private final JDA jda;
	private final UserPreferenceService userPreferenceService;
	private final BotConfig botConfig;
	private final DbHelper dbHelper;
	private final QOTWSubmissionRepository qotwSubmissionRepository;
	private final QuestionQueueRepository questionQueueRepository;

	/**
	 * Checks that there's a question in the QOTW queue ready for posting soon.
	 */
	@Scheduled(cron = "* 0 16 * * 5")//Friday 16:00
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			QOTWConfig config = botConfig.get(guild).getQotwConfig();
			List<QOTWSubmission> submissions = new SubmissionManager(config, dbHelper, qotwSubmissionRepository, questionQueueRepository).getActiveSubmissionThreads(guild.getIdLong());
			for (QOTWSubmission submission : submissions) {
				UserPreference preference = userPreferenceService.getOrCreate(submission.getAuthorId(), Preference.QOTW_REMINDER);
				if (preference.isEnabled()) {
					TextChannel channel = config.getSubmissionChannel();
					channel.getThreadChannels().stream().filter(t -> t.getIdLong() == submission.getThreadId()).forEach(t -> {
						if (t.getMessageCount() <= 1) {
							t.sendMessageFormat("**Question of the Week Reminder**\nHey <@%s>! You still have some time left to submit your answer!", submission.getAuthorId())
									.queue();
						}
					});
				}
			}
		}
	}
}
