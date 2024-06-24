package net.discordjug.javabot.systems.staff_commands.tags.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/tag` command. This holds commands for interacting with "Custom Tags".
 */
public class TagsCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param tagViewSubcommand /tag view
	 * @param tagListSubcommand /tag list
	 * @param tagSearchSubcommand /tag search
	 */
	public TagsCommand(TagViewSubcommand tagViewSubcommand, TagListSubcommand tagListSubcommand, TagSearchSubcommand tagSearchSubcommand) {
		setCommandData(Commands.slash("tag", "Commands for interacting with Custom Tags.")
				.setGuildOnly(true)
		);
		addSubcommands(tagViewSubcommand, tagListSubcommand, tagSearchSubcommand);
	}
}