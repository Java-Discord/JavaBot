package net.javadiscord.javabot.systems.moderation.timeout;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.AddTimeoutSubcommand;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.RemoveTimeoutSubcommand;

/**
 * Handler class for all timeout specific commands.
 */
public class TimeoutCommand extends SlashCommand {

	public TimeoutCommand() {
		setSlashCommandData(Commands.slash("timeout", "Commands for managing User Timeouts."));
		addSubcommands(new AddTimeoutSubcommand(), new RemoveTimeoutSubcommand());
	}
}
