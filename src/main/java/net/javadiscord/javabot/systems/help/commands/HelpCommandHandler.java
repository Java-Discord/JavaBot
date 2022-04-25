package net.javadiscord.javabot.systems.help.commands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.help.commands.subcommands.ExperienceLeaderboardSubcommand;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpAccountSubcommand;
import net.javadiscord.javabot.systems.help.commands.subcommands.ThanksLeaderboardSubcommand;

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
		this.addSubcommand("thanks-leaderboard", new ThanksLeaderboardSubcommand());
		this.addSubcommand("experience-leaderboard", new ExperienceLeaderboardSubcommand());
	}
}
