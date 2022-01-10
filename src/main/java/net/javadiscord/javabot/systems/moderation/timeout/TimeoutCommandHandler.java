package net.javadiscord.javabot.systems.moderation.timeout;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.AddTimeoutSubCommand;
import net.javadiscord.javabot.systems.moderation.timeout.subcommands.RemoveTimeoutSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardAllWarnsSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardWarnByIdSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.WarnAddSubCommand;

public class TimeoutCommandHandler extends DelegatingCommandHandler {
	public TimeoutCommandHandler() {
		addSubcommand("add", new AddTimeoutSubCommand());
		addSubcommand("remove", new RemoveTimeoutSubCommand());
	}
}
