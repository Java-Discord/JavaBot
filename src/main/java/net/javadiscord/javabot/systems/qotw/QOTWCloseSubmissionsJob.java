package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.stream.Collectors;

/**
 * Job which disables the Submission button.
 */
public class QOTWCloseSubmissionsJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		for (var guild : jda.getGuilds()) {
			var config = Bot.config.get(guild);
			var qotwConfig = config.getQotw();
			if (config.getModeration().getLogChannel() == null) continue;
			if (qotwConfig.getSubmissionChannel() == null || qotwConfig.getQuestionChannel() == null) continue;
			var message = getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda);
			if (message == null) continue;
			message.editMessageComponents(
					ActionRow.of(message.getButtons()
							.stream()
							.map(Button::asDisabled)
							.collect(Collectors.toList()))).queue();
			for (var thread : qotwConfig.getSubmissionChannel().getThreadChannels()) {
				thread.getManager().setInvitable(false).setLocked(true).setArchived(true).queue();
			}
		}
	}

	private Message getLatestQOTWMessage(MessageChannel channel, QOTWConfig config, JDA jda) {
		var history = channel.getHistory();
		Message message = null;
		while (message == null) {
			var messages = history.retrievePast(100).complete();
			for (var m : messages) {
				if (m.getAuthor().equals(jda.getSelfUser()) && m.getContentRaw().equals(config.getQOTWRole().getAsMention())) {
					message = m;
					break;
				}
			}
		}
		return message;
	}
}
