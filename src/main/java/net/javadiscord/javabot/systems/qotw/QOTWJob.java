package net.javadiscord.javabot.systems.qotw;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.tasks.jobs.DiscordApiJob;
import net.javadiscord.javabot.util.GuildUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.SQLException;
import java.time.OffsetDateTime;

/**
 * Job which posts a new question to the QOTW channel.
 */
@Slf4j
public class QOTWJob extends DiscordApiJob {
	@Override
	protected void execute(JobExecutionContext context, JDA jda) throws JobExecutionException {
		for (var guild : jda.getGuilds()) {
			if (Bot.config.get(guild).getModeration().getLogChannel() == null) continue;
			try (var c = Bot.dataSource.getConnection()) {
				var repo = new QuestionQueueRepository(c);
				var nextQuestion = repo.getNextQuestion(guild.getIdLong());
				if (nextQuestion.isEmpty()) {
					GuildUtils.getLogChannel(guild).sendMessage("Warning! @here No available next question for QOTW!").queue();
				} else {
					var question = nextQuestion.get();
					var config = Bot.config.get(guild).getQotw();
					if (question.getQuestionNumber() == null) {
						question.setQuestionNumber(repo.getNextQuestionNumber());
					}
					var questionChannel = config.getQuestionChannel();
					if (questionChannel == null) continue;
					questionChannel.sendMessage(config.getQOTWRole().getAsMention())
							.setEmbeds(buildEmbed(question))
							.setActionRows(ActionRow.of(Button.success("qotw-submission:submit:" + question.getQuestionNumber(), "Submit your Answer")))
							.queue(msg -> questionChannel.crosspostMessageById(msg.getIdLong()).queue());
					repo.markUsed(question);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				GuildUtils.getLogChannel(guild).sendMessageFormat("Warning! @here Could not send next QOTW question:\n```\n%s\n```\n", e.getMessage()).queue();
				throw new JobExecutionException(e);
			}
		}
	}

	private MessageEmbed buildEmbed(QOTWQuestion question) {
		var checkTime = OffsetDateTime.now().plusDays(6).withHour(22).withMinute(0).withSecond(0);
		return new EmbedBuilder()
				.setTitle("Question of the Week #" + question.getQuestionNumber())
				.setDescription(String.format("%s\n\nClick the button below to submit your answer.\nYour answers will be checked by <t:%d:F>",
						question.getText(), checkTime.toEpochSecond()))
				.build();
	}
}
