package net.javadiscord.javabot.systems.moderation.timeout;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;

/**
 * Handler class for all timeout specific commands.
 */
public class TimeoutCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public TimeoutCommand() {
		setModerationSlashCommandData(Commands.slash("timeout", "Commands for managing member timeouts."));
		addSubcommands(new AddTimeoutSubcommand(), new RemoveTimeoutSubcommand());
	}
}
