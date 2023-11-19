package net.discordjug.javabot.systems.staff_commands.tags.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.tags.dao.CustomTagRepository;
import net.discordjug.javabot.systems.staff_commands.tags.model.CustomTag;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * <h3>This class represents the /tags command.</h3>
 */
public class TagListSubcommand extends TagsSubcommand {
	private final ExecutorService asyncPool;
	private final CustomTagRepository customTagRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param customTagRepository Dao object that represents the CUSTOM_COMMANDS SQL Table.
	 */
	public TagListSubcommand(BotConfig botConfig, ExecutorService asyncPool, CustomTagRepository customTagRepository) {
		super(botConfig);
		this.asyncPool = asyncPool;
		this.customTagRepository = customTagRepository;
		setCommandData(new SubcommandData("list", "Lists all custom tags"));
		setRequiredStaff(false);
	}

	@Override
	public ReplyCallbackAction handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		asyncPool.execute(()->{
			try {
				List<CustomTag> tags = customTagRepository.getCustomTagsByGuildId(event.getGuild().getIdLong());
				String tagList = tags.stream().map(CustomTag::getName).map(MarkdownUtil::monospace).collect(Collectors.joining(", "));
				Responses.info(event.getHook(), "Custom Tag List",
								String.format(tagList.length() > 0 ? tagList : "No Custom Tags created yet.")).queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, TagListSubcommand.class.getSimpleName());
			}
		});
		return event.deferReply(false);
	}
}
