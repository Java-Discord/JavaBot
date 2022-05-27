package net.javadiscord.javabot.systems.moderation.warn;

import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardAllWarnsSubcommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardWarnByIdSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.WarnAddSubcommand;

/**
 * Handler class for all commands regarding the warn system.
 */
public class WarnCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public WarnCommandHandler() {
		addSubcommand("add", new WarnAddSubcommand());
		addSubcommand("discard-by-id", new DiscardWarnByIdSubCommand());
		addSubcommand("discard-all", new DiscardAllWarnsSubcommand());
	}
}
