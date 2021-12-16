package com.javadiscord.javabot.service.qotw;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.service.qotw.dao.QuestionRepository;
import com.javadiscord.javabot.service.qotw.model.QOTWQuestion;
import com.javadiscord.javabot.service.schedule.jobs.DiscordApiJob;
import com.javadiscord.javabot.utils.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.SQLException;
import java.time.OffsetDateTime;

/**
 * Job which posts a new question to the QOTW channel.
 */
public class QOTWJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		for (var guild : jda.getGuilds()) {
			try (var c = Bot.dataSource.getConnection()) {
				var repo = new QuestionRepository(c);
				var nextQuestion = repo.getNextQuestion(guild.getIdLong());
				if (nextQuestion.isEmpty()) {
					Misc.sendToLog(guild, "Warning! @here No available next question for QOTW!");
				} else {
					var question = nextQuestion.get();
					var config = Bot.config.get(guild).getQotw();
					if (question.getQuestionNumber() == null) {
						question.setQuestionNumber(repo.getNextQuestionNumber());
					}
					var questionChannel = config.getQuestionChannel();
					questionChannel.sendMessageEmbeds(buildEmbed(question, jda)).queue(msg -> {
						questionChannel.crosspostMessageById(msg.getIdLong()).queue();
						questionChannel.sendMessage("@question-ping").queue();
					});
					repo.markUsed(question);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				Misc.sendToLogFormat(guild, "Warning! @here Could not send next QOTW question:\n```\n%s\n```\n", e.getMessage());
				throw new JobExecutionException(e);
			}
		}
	}

	private MessageEmbed buildEmbed(QOTWQuestion question, JDA jda) {
		OffsetDateTime checkTime = OffsetDateTime.now().plusDays(6).withHour(22).withMinute(0).withSecond(0);

		String description = String.format(
				"**%s**\n\nDM your answer to <@%d>.\nYour answers will be checked by <t:%d:F>",
				question.getText(),
				jda.getSelfUser().getIdLong(),
				checkTime.toEpochSecond()
		);

		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(description)
				.build();
	}
}
