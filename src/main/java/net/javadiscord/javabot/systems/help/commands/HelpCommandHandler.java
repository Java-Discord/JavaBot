package net.javadiscord.javabot.systems.help.commands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.commands.subcommands.leaderboard.ExperienceLeaderboardSubcommand;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpAccountSubcommand;
import net.javadiscord.javabot.systems.commands.subcommands.leaderboard.ThanksLeaderboardSubcommand;

/**
 * Handler class for all Help Commands.
 */
public class HelpCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands.
	 * {@link DelegatingCommandHandler#addSubcommand}
	 */
	public HelpCommandHandler() {
		this.addSubcommand("account", new HelpAccountSubcommand());
	}
}
