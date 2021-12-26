package net.javadiscord.javabot.systems.moderation.warn;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardAllWarnsSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardWarnByIdSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.WarnAddSubCommand;

public class WarnCommandHandler extends DelegatingCommandHandler {
	public WarnCommandHandler() {
		addSubcommand("add", new WarnAddSubCommand());
		addSubcommand("discard-by-id", new DiscardWarnByIdSubCommand());
		addSubcommand("discard-all", new DiscardAllWarnsSubCommand());
	}
}
