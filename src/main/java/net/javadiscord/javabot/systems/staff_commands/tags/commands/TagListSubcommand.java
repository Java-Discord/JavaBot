package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.staff_commands.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.staff_commands.tags.model.CustomTag;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h3>This class represents the /tags command.</h3>
 */
public class TagListSubcommand extends TagsSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public TagListSubcommand() {
		setSubcommandData(new SubcommandData("list", "Lists all custom tags"));
	}

	@Override
	public ReplyCallbackAction handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		DbHelper.doDaoAction(CustomTagRepository::new, dao -> {
			List<CustomTag> tags = dao.getCustomTagsByGuildId(event.getGuild().getIdLong());
			String tagList = tags.stream().map(CustomTag::getName).map(MarkdownUtil::monospace).collect(Collectors.joining(", "));
			Responses.info(event.getHook(), "Custom Tag List",
							String.format(tagList.length() > 0 ? tagList : "No Custom Tags created yet.")).queue();
		});
		return event.deferReply(false);
	}
}
