package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.staff_commands.tags.CustomTagManager;

/**
 * Represents the `/tag` command. This holds commands for interacting with "Custom Tags".
 */
public class TagsCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param tagManager The {@link CustomTagManager}
	 */
	public TagsCommand(CustomTagManager tagManager) {
		setSlashCommandData(Commands.slash("tag", "Commands for interacting with Custom Tags.")
				.setGuildOnly(true)
		);
		addSubcommands(new TagViewSubcommand(tagManager), new TagListSubcommand());
	}
}