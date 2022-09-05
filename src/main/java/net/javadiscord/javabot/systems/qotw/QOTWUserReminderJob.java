package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionManager;
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

	/**
	 * Checks that there's a question in the QOTW queue ready for posting soon.
	 */
	@Scheduled(cron = "* 0 16 * * 5")//Friday 16:00
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			QOTWConfig config = Bot.getConfig().get(guild).getQotwConfig();
			List<QOTWSubmission> submissions = new SubmissionManager(config).getActiveSubmissionThreads(guild.getIdLong());
			for (QOTWSubmission submission : submissions) {
				UserPreferenceService manager = new UserPreferenceService(Bot.getDataSource());
				UserPreference preference = manager.getOrCreate(submission.getAuthorId(), Preference.QOTW_REMINDER);
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
