package net.javadiscord.javabot.systems.qotw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.NewsChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

/**
 * Job which posts a new question to the QOTW channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QOTWJob {
	private final JDA jda;
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final DataSource dataSource;

	/**
	 * Posts a new question to the QOTW channel.
	 * @throws SQLException if an SQL error occurs
	 */
	@Scheduled(cron = "0 0 9 * * 0")//MONDAY, 09:00
	public void execute() throws SQLException {
		for (Guild guild : jda.getGuilds()) {
			if (guild.getBoostTier() == Guild.BoostTier.TIER_1) {
				log.error("Guild {} does not have access to private threads. ({})", guild.getName(), guild.getBoostTier().name());
				return;
			}
			GuildConfig config = botConfig.get(guild);
			if (config.getModerationConfig().getLogChannel() == null) continue;
			try (Connection c = dataSource.getConnection()) {
				QuestionQueueRepository repo = new QuestionQueueRepository(c);
				Optional<QOTWQuestion> nextQuestion = repo.getNextQuestion(guild.getIdLong());
				if (nextQuestion.isEmpty()) {
					notificationService.withGuild(guild).sendToModerationLog(m -> m.sendMessageFormat("Warning! %s No available next question for QOTW!", config.getQotwConfig().getQOTWReviewRole().getAsMention()));
				} else {
					QOTWQuestion question = nextQuestion.get();
					QOTWConfig qotw = config.getQotwConfig();
					qotw.getSubmissionChannel().getThreadChannels().forEach(thread -> thread.getManager().setLocked(true).setArchived(true).queue());
					qotw.getSubmissionChannel().getManager()
							.putRolePermissionOverride(guild.getIdLong(), Set.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS), Collections.singleton(Permission.MESSAGE_SEND))
							.queue();
					if (question.getQuestionNumber() == null) {
						question.setQuestionNumber(repo.getNextQuestionNumber());
					}
					NewsChannel questionChannel = qotw.getQuestionChannel();
					if (questionChannel == null) continue;
					questionChannel.sendMessage(qotw.getQOTWRole().getAsMention())
							.setEmbeds(this.buildQuestionEmbed(question))
							.setActionRows(ActionRow.of(Button.success("qotw-submission:submit:" + question.getQuestionNumber(), "Submit your Answer")))
							.queue(msg -> questionChannel.crosspostMessageById(msg.getIdLong()).queue());
					repo.markUsed(question);
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				notificationService.withGuild(guild).sendToModerationLog(c -> c.sendMessageFormat("Warning! %s Could not send next QOTW question:\n```\n%s\n```\n", config.getQotwConfig().getQOTWReviewRole().getAsMention(), e.getMessage()));
				throw e;
			}
		}
	}

	private @NotNull MessageEmbed buildQuestionEmbed(@NotNull QOTWQuestion question) {
		OffsetDateTime checkTime = OffsetDateTime.now().plusDays(6).withHour(22).withMinute(0).withSecond(0);
		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(String.format("%s\n\nClick the button below to submit your answer.\nYour answers will be checked by <t:%d:F>",
						question.getText(), checkTime.toEpochSecond()))
				.build();
	}
}
