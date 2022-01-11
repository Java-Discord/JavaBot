package net.javadiscord.javabot.systems.moderation.timeout;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.AddTimeoutSubCommand;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.RemoveTimeoutSubCommand;

public class TimeoutCommandHandler extends DelegatingCommandHandler {
	public TimeoutCommandHandler() {
		addSubcommand("add", new AddTimeoutSubCommand());
		addSubcommand("remove", new RemoveTimeoutSubCommand());
	}
}
