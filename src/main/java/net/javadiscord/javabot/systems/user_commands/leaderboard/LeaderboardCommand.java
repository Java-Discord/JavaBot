package net.javadiscord.javabot.systems.user_commands.leaderboard;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/leaderboard` command. This holds commands viewing all the server's different leaderboards.
 */
public class LeaderboardCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
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
