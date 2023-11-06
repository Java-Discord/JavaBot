package net.javadiscord.javabot.systems.user_commands.leaderboard;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/leaderboard` command. This holds commands viewing all the server's different leaderboards.
 */
public class LeaderboardCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param qotwLeaderboardSubcommand /leaderboard qotw
	 * @param thanksLeaderboardSubcommand /leaderboard thanks
	 * @param experienceLeaderboardSubcommand /leaderboard help-experience
	 */
	public LeaderboardCommand(QOTWLeaderboardSubcommand qotwLeaderboardSubcommand, ThanksLeaderboardSubcommand thanksLeaderboardSubcommand, ExperienceLeaderboardSubcommand experienceLeaderboardSubcommand) {
		setCommandData(Commands.slash("leaderboard", "Command for all leaderboards.")
				.setGuildOnly(true)
		);
		addSubcommands(qotwLeaderboardSubcommand, thanksLeaderboardSubcommand, experienceLeaderboardSubcommand);
	}
}
