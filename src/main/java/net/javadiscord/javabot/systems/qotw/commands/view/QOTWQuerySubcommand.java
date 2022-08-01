package net.javadiscord.javabot.systems.qotw.commands.view;

import java.util.Comparator;
import java.util.List;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.Responses;

/**
 * Represents the `/qotw-view query` subcommand. It allows for listing filtering QOTWs.
 */
public class QOTWQuerySubcommand extends SlashCommand.Subcommand {
	public QOTWQuerySubcommand() {
		setSubcommandData(new SubcommandData("list-questions", "Lists previous questions of the week")
				.addOption(OptionType.STRING, "query", "Only queries questions that contain a specific query", false));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if(!event.isFromGuild()) {
			Responses.replyGuildOnly(event).setEphemeral(true).queue();
			return;
		}
		String query = event.getOption("query", ()->"", OptionMapping::getAsString);
		event.deferReply(true).queue();
		DbActions.doAsyncDaoAction(QuestionQueueRepository::new, repo->{
			List<QOTWQuestion> questions = repo.getUsedQuestionsWithQuery(event.getGuild().getIdLong(), query, 0, 20);
			EmbedBuilder eb = new EmbedBuilder();
			eb.setDescription("Questions of the week"+(query.isEmpty()?"":" matching '"+query+"'"));
			questions
					.stream()
					.sorted(Comparator.comparingInt(QOTWQuestion::getQuestionNumber))
					.map(q -> new MessageEmbed.Field("Question #" + q.getQuestionNumber(), q.getText(), true))
					.forEach(eb::addField);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		});
	}
}
