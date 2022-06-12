package net.javadiscord.javabot.systems.user_commands.leaderboard;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.user_commands.leaderboard.subcommands.ExperienceLeaderboardSubcommand;
import net.javadiscord.javabot.systems.user_commands.leaderboard.subcommands.QOTWLeaderboardSubcommand;
import net.javadiscord.javabot.systems.user_commands.leaderboard.subcommands.ThanksLeaderboardSubcommand;

/**
 * Single command housing all leaderboards.
 */
public class LeaderboardCommand extends SlashCommand {

	public LeaderboardCommand() {
		setSlashCommandData(Commands.slash("leaderboard", "Command for all leaderboards.")
				.setGuildOnly(true)
		);
		addSubcommands(
				new QOTWLeaderboardSubcommand(),
				new ThanksLeaderboardSubcommand(),
				new ExperienceLeaderboardSubcommand());
	}
}
