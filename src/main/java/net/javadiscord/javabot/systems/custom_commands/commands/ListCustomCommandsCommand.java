package net.javadiscord.javabot.systems.custom_commands.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.custom_commands.dao.CustomCommandRepository;
import net.javadiscord.javabot.systems.custom_commands.model.CustomCommand;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Command that lists Custom Slash Commands.
 */
public class ListCustomCommandsCommand extends SlashCommand {
	public ListCustomCommandsCommand() {
		setSlashCommandData(Commands.slash("customcommands", "Lists all custom commands")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.deferReply(false).queue();
		DbHelper.doDaoAction(CustomCommandRepository::new, dao -> {
			List<CustomCommand> commands = dao.getCustomCommandsByGuildId(event.getGuild().getIdLong());
			StringBuilder sb = new StringBuilder();
			for (CustomCommand c : commands) {
				sb.append("/").append(c.getName()).append("\n");
			}
			Responses.success(event.getHook(),
					"Custom Slash Command List",
					String.format("```\n%s\n```", sb.length() > 0 ? sb : "No Custom Commands created yet.")
			).queue();
		});
	}
}
