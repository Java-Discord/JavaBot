package net.javadiscord.javabot.systems.qotw.commands.view;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.AutoDetectableComponentHandler;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the `/qotw-view query` subcommand. It allows for listing filtering QOTWs.
 */
@AutoDetectableComponentHandler("qotw-list-questions")
public class QOTWQuerySubcommand extends SlashCommand.Subcommand implements ButtonHandler {

	private static final int MAX_BUTTON_QUERY_LENGTH = 10;
	private static final int PAGE_LIMIT = 20;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public QOTWQuerySubcommand() {
		setSubcommandData(new SubcommandData("list-questions", "Lists previous 'Questions of the Week'")
				.addOption(OptionType.STRING, "query", "Only queries questions that contain a specific query", false)
				.addOption(OptionType.INTEGER, "page", "The page to show, starting with 1", false)
		);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			Responses.replyGuildOnly(event).setEphemeral(true).queue();
			return;
		}
		String query = event.getOption("query", "", OptionMapping::getAsString);
		int page = event.getOption("page", 1, OptionMapping::getAsInt) - 1;
		if (page < 0) {
			Responses.error(event, "The page must be equal to or greater than 1!").queue();
			return;
		}
		event.deferReply(true).queue();
		DbActions.doAsyncDaoAction(QuestionQueueRepository::new, repo -> {
			MessageEmbed embed = buildListQuestionsEmbed(repo, event.getGuild().getIdLong(), query, page);
			event.getHook()
					.sendMessageEmbeds(embed)
					.addActionRows(buildPageControls(query, page, embed))
					.queue();
		});
	}

	@Override
	public void handleButton(@Nonnull ButtonInteractionEvent event, @Nonnull Button button) {
		event.deferEdit().queue();
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		int page = Integer.parseInt(id[1]);
		String query = id.length == 2 ? "" : id[2];
		if (page < 0) {
			Responses.error(event.getHook(), "The page must be equal to or greater than 1!").queue();
			return;
		}
		DbHelper.doDaoAction(QuestionQueueRepository::new, repo -> {
			MessageEmbed embed = buildListQuestionsEmbed(repo, event.getGuild().getIdLong(), query, page);
			event.getHook()
					.editOriginalEmbeds(embed)
					.setActionRows(buildPageControls(query, page, embed))
					.queue();
		});
	}

	@NotNull
	private ActionRow buildPageControls(@NotNull String query, int page, MessageEmbed embed) {
		if (query.length() > MAX_BUTTON_QUERY_LENGTH) {
			query = query.substring(0, MAX_BUTTON_QUERY_LENGTH);
		}
		return ActionRow.of(
				Button.primary(ComponentIdBuilder.build("qotw-list-questions", page - 1 + "", query), "Previous Page")
						.withDisabled(page <= 0),
				Button.primary(ComponentIdBuilder.build("qotw-list-questions", page + 1 + "", query), "Next Page")
						.withDisabled(embed.getFields().size() < PAGE_LIMIT)
		);
	}

	private @NotNull MessageEmbed buildListQuestionsEmbed(@NotNull QuestionQueueRepository repo, long guildId, String query, int page) throws SQLException {
		List<QOTWQuestion> questions = repo.getUsedQuestionsWithQuery(guildId, query, page * PAGE_LIMIT, PAGE_LIMIT);
		EmbedBuilder eb = new EmbedBuilder()
				.setDescription("**Questions of the Week" + (query.isEmpty() ? "" : " matching '" + query + "'") + "**")
				.setColor(Responses.Type.DEFAULT.getColor())
				.setFooter("Page " + (page + 1));
		questions.stream()
				.sorted(Comparator.comparingInt(QOTWQuestion::getQuestionNumber))
				.map(q -> new MessageEmbed.Field("Question #" + q.getQuestionNumber(), q.getText(), true))
				.forEach(eb::addField);
		if (eb.getFields().isEmpty()) {
			eb.appendDescription("\nNo questions found");
			if (page != 0) {
				eb.appendDescription(" on this page");
			}
		}
		return eb.build();
	}
}
