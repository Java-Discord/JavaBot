package net.javadiscord.javabot.systems.moderation.timeout;

import net.javadiscord.javabot.systems.moderation.timeout.subcommands.AddTimeoutSubcommand;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.RemoveTimeoutSubcommand;

/**
 * Handler class for all timeout specific commands.
 */
public class TimeoutCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public TimeoutCommandHandler() {
		addSubcommand("add", new AddTimeoutSubcommand());
		addSubcommand("remove", new RemoveTimeoutSubcommand());
	}
}
