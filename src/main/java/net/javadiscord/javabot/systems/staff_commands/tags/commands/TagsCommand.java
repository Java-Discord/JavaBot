package net.javadiscord.javabot.systems.staff_commands.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/tag` command. This holds commands for interacting with "Custom Tags".
 */
public class TagsCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public TagsCommand() {
		setSlashCommandData(Commands.slash("tag", "Commands for interacting with Custom Tags."));
		addSubcommands(new TagViewSubcommand(), new TagListSubcommand());
	}
}