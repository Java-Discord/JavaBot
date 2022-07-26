package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionControlsManager;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Job which disables the Submission button.
 */
public class QOTWCloseSubmissionsJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, @NotNull JDA jda) throws JobExecutionException {
		for (Guild guild : jda.getGuilds()) {
			// Disable 'Submit your Answer' button on latest QOTW
			GuildConfig config = Bot.getConfig().get(guild);
			QOTWConfig qotwConfig = config.getQotwConfig();
			qotwConfig.getSubmissionChannel().getManager()
					.putRolePermissionOverride(guild.getIdLong(), Collections.emptySet(), Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS))
					.queue();
			if (config.getModerationConfig().getLogChannel() == null) continue;
			if (qotwConfig.getSubmissionChannel() == null || qotwConfig.getQuestionChannel() == null) continue;
			Message message = getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda);
			if (message == null) continue;
			message.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:closed", "Submissions closed").asDisabled())).queue();
			for (ThreadChannel thread : qotwConfig.getSubmissionChannel().getThreadChannels()) {
				try (Connection con = Bot.getDataSource().getConnection()) {
					QOTWSubmissionRepository repo = new QOTWSubmissionRepository(con);
					Optional<QOTWSubmission> optionalSubmission = repo.getSubmissionByThreadId(thread.getIdLong());
					if (optionalSubmission.isEmpty()) continue;
					new SubmissionControlsManager(thread.getGuild(), optionalSubmission.get()).sendControls();
				} catch (SQLException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					throw new JobExecutionException(e);
				}
			}
		}
	}

	private Message getLatestQOTWMessage(MessageChannel channel, QOTWConfig config, JDA jda) {
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
