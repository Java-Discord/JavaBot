package net.javadiscord.javabot.systems.moderation.timeout;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.AddTimeoutSubcommand;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.RemoveTimeoutSubCommand;

public class TimeoutCommandHandler extends DelegatingCommandHandler {
	public TimeoutCommandHandler() {
		addSubcommand("add", new AddTimeoutSubcommand());
		addSubcommand("remove", new RemoveTimeoutSubCommand());
	}
}
