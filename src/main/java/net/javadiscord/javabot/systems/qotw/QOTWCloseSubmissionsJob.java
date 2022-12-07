package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Job which disables the Submission button.
 */
@Service
@RequiredArgsConstructor
public class QOTWCloseSubmissionsJob {
	private final JDA jda;
	private final NotificationService notificationService;
	private final BotConfig botConfig;

	/**
	 * Disables the submission button.
	 *
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 21 * * 7")//Sunday 21:00
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			// Disable 'Submit your Answer' button on latest QOTW
			GuildConfig config = botConfig.get(guild);
			QOTWConfig qotwConfig = config.getQotwConfig();
			qotwConfig.getSubmissionChannel().getManager()
					.putRolePermissionOverride(guild.getIdLong(), Collections.emptySet(), Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS))
					.queue();
			if (config.getModerationConfig().getLogChannel() == null) continue;
			if (qotwConfig.getSubmissionChannel() == null || qotwConfig.getQuestionChannel() == null) continue;
			getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda)
					.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:closed", "Submissions closed").asDisabled())).queue();
			notificationService.withGuild(guild)
					.sendToMessageLog(log ->
							log.sendMessageFormat("%s%nIt's review time! There are %s threads to review!",
									qotwConfig.getQOTWReviewRole().getAsMention(), countThreads(qotwConfig))
					);
		}
	}

	private long countThreads(@NotNull QOTWConfig qotwConfig) {
		return qotwConfig.getSubmissionChannel().getThreadChannels().size();
	}

	private Message getLatestQOTWMessage(@NotNull MessageChannel channel, QOTWConfig config, JDA jda) {
		MessageHistory history = channel.getHistory();
		Message message = null;
		while (message == null) {
			List<Message> messages = history.retrievePast(100).complete();
			for (Message m : messages) {
				if (m.getAuthor().equals(jda.getSelfUser()) && m.getContentRaw().equals(config.getQOTWRole().getAsMention())) {
					message = m;
					break;
				}
			}
		}
		return message;
	}
}
