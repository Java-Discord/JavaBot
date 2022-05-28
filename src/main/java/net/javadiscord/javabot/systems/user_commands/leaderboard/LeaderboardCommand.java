package net.javadiscord.javabot.systems.user_commands.leaderboard;

import net.javadiscord.javabot.systems.user_commands.leaderboard.subcommands.ExperienceLeaderboardSubcommand;
import net.javadiscord.javabot.systems.user_commands.leaderboard.subcommands.QOTWLeaderboardSubcommand;
import net.javadiscord.javabot.systems.user_commands.leaderboard.subcommands.ThanksLeaderboardSubcommand;

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
