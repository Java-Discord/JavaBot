package net.javadiscord.javabot.systems.custom_commands.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/custom-commands` command. This holds administrative commands for managing "Custom Commands".
 */
public class CustomCommandsCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public CustomCommandsCommand() {
		setSlashCommandData(Commands.slash("customcommands-admin", "Administrative commands for managing \"Custom Commands\".")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new CreateCustomCommandSubcommand(), new DeleteCustomCommandSubcommand(), new EditCustomCommandSubcommand());
	}
}