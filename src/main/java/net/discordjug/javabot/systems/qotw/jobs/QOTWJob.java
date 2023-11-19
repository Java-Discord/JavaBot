package net.discordjug.javabot.systems.qotw.jobs;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.guild.QOTWConfig;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.discordjug.javabot.systems.qotw.model.QOTWQuestion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Job which posts a new question to the QOTW channel.
 */
@Service
@RequiredArgsConstructor
public class QOTWJob {
	private final JDA jda;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final QuestionQueueRepository questionQueueRepository;

	/**
	 * Posts a new question to the QOTW channel.
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 9 * * 1") // Monday, 09:00  UTC
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			GuildConfig config = botConfig.get(guild);
			if (config.getModerationConfig().getLogChannel() == null) {
				continue;
			}
			Optional<QOTWQuestion> nextQuestion = questionQueueRepository.getNextQuestion(guild.getIdLong());
			if (nextQuestion.isEmpty()) {
				notificationService.withGuild(guild).sendToModerationLog(m -> m.sendMessageFormat("Warning! %s No available next question for QOTW!", config.getQotwConfig().getQOTWReviewRole().getAsMention()));
			} else {
				QOTWQuestion question = nextQuestion.get();
				QOTWConfig qotw = config.getQotwConfig();
				qotw.getSubmissionChannel().getThreadChannels().forEach(thread -> {
					notificationService.withGuild(guild).sendToModerationLog(log -> log.sendMessageFormat("Closed unreviewed submission thread %s", thread.getAsMention()));
					thread.getManager().setLocked(true).setArchived(true).queue();
				});
				qotw.getSubmissionChannel().getManager()
						.putRolePermissionOverride(guild.getIdLong(), Set.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS), Collections.singleton(Permission.MESSAGE_SEND))
						.queue();
				if (question.getQuestionNumber() == null) {
					question.setQuestionNumber(questionQueueRepository.getNextQuestionNumber());
				}
				NewsChannel questionChannel = qotw.getQuestionChannel();
				if (questionChannel != null) {
					questionChannel.sendMessage(qotw.getQOTWRole().getAsMention())
							.setEmbeds(this.buildQuestionEmbed(question))
							.setComponents(ActionRow.of(Button.success("qotw-submission:submit:" + question.getQuestionNumber(), "Submit your Answer")))
							.queue(msg -> questionChannel.crosspostMessageById(msg.getIdLong()).queue());
					questionQueueRepository.markUsed(question);
				}
			}
		}
	}

	private @NotNull MessageEmbed buildQuestionEmbed(@NotNull QOTWQuestion question) {
		OffsetDateTime checkTime = OffsetDateTime.now().plusDays(6).withHour(22).withMinute(0).withSecond(0);
		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(String.format("%s%n%nClick the button below to submit your answer.%nYour answer will be checked by <t:%d:F>%nUse of generative AI tools like ChatGPT is __not__ allowed",
						question.getText(), checkTime.toEpochSecond()))
				.build();
	}
}
