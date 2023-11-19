package net.discordjug.javabot.systems.moderation.timeout;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.systems.moderation.CommandModerationPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Handler class for all timeout specific commands.
 */
public class TimeoutCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param addTimeoutSubcommand /timeout add
	 * @param removeTimeoutSubcommand /timeout remove
	 */
	public TimeoutCommand(AddTimeoutSubcommand addTimeoutSubcommand, RemoveTimeoutSubcommand removeTimeoutSubcommand) {
		setModerationSlashCommandData(Commands.slash("timeout", "Commands for managing member timeouts."));
		addSubcommands(addTimeoutSubcommand, removeTimeoutSubcommand);
	}
}
