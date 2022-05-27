package net.javadiscord.javabot.systems.commands;

import net.javadiscord.javabot.systems.commands.subcommands.leaderboard.ExperienceLeaderboardSubcommand;
import net.javadiscord.javabot.systems.commands.subcommands.leaderboard.ThanksLeaderboardSubcommand;
import net.javadiscord.javabot.systems.commands.subcommands.leaderboard.QOTWLeaderboardSubcommand;

/**
 * Single command housing all leaderboards.
 */
public class LeaderboardCommand extends DelegatingCommandHandler {
	/**
	 * Leaderboard command handler.
	 */
	public LeaderboardCommand() {
		this.addSubcommand("qotw", new QOTWLeaderboardSubcommand());
		this.addSubcommand("thanks", new ThanksLeaderboardSubcommand());
		this.addSubcommand("help-xp", new ExperienceLeaderboardSubcommand());
	}
}
