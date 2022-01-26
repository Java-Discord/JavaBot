package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Instant;

/**
 * Job which disables the Submission button.
 */
public class QOTWCloseSubmissionsJob extends DiscordApiJob {
	private final String SUBMISSION_PENDING = "\uD83D\uDD52";

	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		for (var guild : jda.getGuilds()) {
			// Disable 'Submit your Answer' button on latest QOTW
			var config = Bot.config.get(guild);
			var qotwConfig = config.getQotw();
			if (config.getModeration().getLogChannel() == null) continue;
			if (qotwConfig.getSubmissionChannel() == null || qotwConfig.getQuestionChannel() == null) continue;
			var message = getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda);
			if (message == null) continue;
			message.editMessageComponents(
					ActionRow.of(Button.secondary("qotw-submission:closed", "Submissions closed").asDisabled()))
					.queue();
			var manager = new SubmissionManager(qotwConfig);
			// Remove Thread Owners and send Submission Controls
			for (var thread : qotwConfig.getSubmissionChannel().getThreadChannels()) {
				var ownerId = manager.getSubmissionThreadOwner(thread).getId();
				thread.getThreadMembers()
						.stream()
						.filter(m -> !m.getMember().getRoles().contains(qotwConfig.getQOTWReviewRole()))
						.forEach(m -> thread.removeThreadMember(m.getUser()).queue());
				thread.getManager().setName(String.format("%s %s", SUBMISSION_PENDING, thread.getName())).queue();
				thread.sendMessage(qotwConfig.getQOTWReviewRole().getAsMention())
						.setEmbeds(buildSubmissionControlEmbed(qotwConfig))
						.setActionRows(ActionRow.of(
								Button.success("submission-controls:accept:" + ownerId, "Accept"),
								Button.danger("submission-controls:decline:" + ownerId, "Decline"),
								Button.danger("submission-controls:delete", "Delete")
						)).queue();
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

	private MessageEmbed buildSubmissionControlEmbed(QOTWConfig config) {
		return new EmbedBuilder()
				.setColor(Bot.config.get(config.getGuild()).getSlashCommand().getDefaultColor())
				.setTitle("Submission Controls")
				.setDescription("Please choose an action for this Submission.")
				.setTimestamp(Instant.now())
				.build();
	}
}