package net.discordjug.javabot.systems.staff_commands.tags.commands;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.tags.dao.CustomTagRepository;
import net.discordjug.javabot.systems.staff_commands.tags.model.CustomTag;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

/**
 * The /tag search command which lists all tags matching a query.
 */
public class TagSearchSubcommand extends TagsSubcommand {
	
	private final ExecutorService asyncPool;
	private final CustomTagRepository customTagRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot.
	 * @param asyncPool Thread pool for asynchronous operations.
	 * @param customTagRepository The repository for accessing tags in the database.
	 */
	public TagSearchSubcommand(BotConfig botConfig, ExecutorService asyncPool, CustomTagRepository customTagRepository) {
		super(botConfig);
		this.asyncPool = asyncPool;
		this.customTagRepository = customTagRepository;
		setCommandData(
			new SubcommandData("search", "Searches for tags using a query")
				.addOption(OptionType.STRING, "query", "The search query", true));
		setRequiredStaff(false);
	}

	@Override
	protected InteractionCallbackAction<?> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event)
			throws SQLException {
		
		String query = event.getOption("query", "", OptionMapping::getAsString);
		
		asyncPool.execute(()->{
			try {
				List<CustomTag> tags = customTagRepository.search(event.getGuild().getIdLong(), query);
				String tagList = tags
						.stream()
						.map(CustomTag::getName)
						.map(MarkdownUtil::monospace)
						.collect(Collectors.joining(", "));
				Responses.info(event.getHook(), "Custom tags containing \"" + query + "\"",
								String.format(tagList.length() > 0 ? tagList : "No Custom Tags have been found.")).queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, TagListSubcommand.class.getSimpleName());
				Responses.error(event.getHook(), "An error occured trying to search for tags").queue();
			}
		});
		return event.deferReply(false);
	}

}
