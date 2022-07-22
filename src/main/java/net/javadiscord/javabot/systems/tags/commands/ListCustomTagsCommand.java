package net.javadiscord.javabot.systems.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.tags.model.CustomTag;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h3>This class represents the /customcommands command.</h3>
 */
public class ListCustomTagsCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public ListCustomTagsCommand() {
		setSlashCommandData(Commands.slash("tags", "Lists all custom tags")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.deferReply(false).queue();
		DbHelper.doDaoAction(CustomTagRepository::new, dao -> {
			List<CustomTag> tags = dao.getCustomTagsByGuildId(event.getGuild().getIdLong());
			String tagList = tags.stream().map(CustomTag::getName).collect(Collectors.joining("\n"));
			Responses.success(event.getHook(), "Custom Tag List",
							String.format("```\n%s\n```", tagList.length() > 0 ? tagList : "No Custom Tags created yet.")).queue();
		});
	}
}
