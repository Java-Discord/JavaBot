package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Job which disables the Submission button.
 */
@Service
@RequiredArgsConstructor
public class QOTWCloseSubmissionsJob {
	private static final String SUBMISSION_PENDING = "\uD83D\uDD52";

	private final JDA jda;
	private final NotificationService notificationService;
	private final QuestionQueueRepository questionQueueRepository;
	private final ExecutorService asyncPool;
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
			Message questionMessage = getLatestQOTWMessage(qotwConfig.getQuestionChannel(), qotwConfig, jda);
			questionMessage.editMessageComponents(ActionRow.of(Button.secondary("qotw-submission:closed", "Submissions closed").asDisabled())).queue();
			notificationService.withGuild(guild)
					.sendToModerationLog(log ->
							log.sendMessageFormat("%s%nIt's review time! There are %s threads to review:\n%s",
									qotwConfig.getQOTWReviewRole().getAsMention(),
									qotwConfig.getSubmissionChannel().getThreadChannels().size(),
									qotwConfig.getSubmissionChannel().getThreadChannels().stream()
											.map(ThreadChannel::getAsMention)
											.collect(Collectors.joining("\n")))
					);
			qotwConfig.getSubmissionChannel().getThreadChannels().forEach(t ->
					t.getManager().setName(SUBMISSION_PENDING + t.getName()).queue());
			if (qotwConfig.getSubmissionsForumChannel() == null) continue;
			asyncPool.execute(() -> {
				MessageEmbed embed = questionMessage.getEmbeds().get(0);
				Optional<QOTWQuestion> questionOptional = questionQueueRepository.findByQuestionNumber(getQuestionNumberFromEmbed(embed));
				if (questionOptional.isPresent()) {
					QOTWQuestion question = questionOptional.get();
					try (MessageCreateData data = new MessageCreateBuilder()
							.setEmbeds(buildQuestionEmbed(question)).build()) {
							qotwConfig.getSubmissionsForumChannel()
									.createForumPost(String.format("#%s — %s", question.getQuestionNumber(), question.getText()), data)
									.queue(f -> f.getThreadChannel().getManager().setPinned(true).queue());
						}
					}
			});
		}
	}

	private @NotNull MessageEmbed buildQuestionEmbed(@NotNull QOTWQuestion question) {
		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(question.getText())
				.build();
	}

	private int getQuestionNumberFromEmbed(@NotNull MessageEmbed embed) {
		return embed.getTitle() == null ? 0 : Integer.parseInt(embed.getTitle().replaceAll("\\D+", ""));
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
