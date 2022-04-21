package net.javadiscord.javabot.systems.qotw;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.NewsChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.QOTWConfig;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;

/**
 * Job which posts a new question to the QOTW channel.
 */
@Slf4j
public class QOTWJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		for (Guild guild : jda.getGuilds()) {
			if (guild.getBoostTier() == Guild.BoostTier.TIER_1) {
				log.error("Guild {} does not have access to private threads. ({})", guild.getName(), guild.getBoostTier().name());
				return;
			}
			GuildConfig config = Bot.config.get(guild);
			if (config.getModeration().getLogChannel() == null) continue;
			try (var c = Bot.dataSource.getConnection()) {
				QuestionQueueRepository repo = new QuestionQueueRepository(c);
				var nextQuestion = repo.getNextQuestion(guild.getIdLong());
				if (nextQuestion.isEmpty()) {
					new GuildNotificationService(guild).sendLogChannelNotification("Warning! %s No available next question for QOTW!", config.getQotw().getQOTWReviewRole().getAsMention());
				} else {
					QOTWQuestion question = nextQuestion.get();
					QOTWConfig qotw = config.getQotw();
					qotw.getSubmissionChannel().getThreadChannels().forEach(thread -> thread.getManager().setLocked(true).queue());
					qotw.getSubmissionChannel().getManager()
							.putRolePermissionOverride(guild.getIdLong(), Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS), Collections.emptySet())
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
				e.printStackTrace();
				new GuildNotificationService(guild).sendLogChannelNotification("Warning! %s Could not send next QOTW question:\n```\n%s\n```\n", config.getQotw().getQOTWReviewRole().getAsMention(), e.getMessage());
				throw new JobExecutionException(e);
			}
		}
	}

	private MessageEmbed buildQuestionEmbed(QOTWQuestion question) {
		var checkTime = OffsetDateTime.now().plusDays(6).withHour(22).withMinute(0).withSecond(0);
		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(String.format("%s\n\nClick the button below to submit your answer.\nYour answers will be checked by <t:%d:F>",
						question.getText(), checkTime.toEpochSecond()))
				.build();
	}
}
