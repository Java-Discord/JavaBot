package net.javadiscord.javabot.systems.user_commands.leaderboard;

import java.util.concurrent.ExecutorService;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;

/**
 * Represents the `/leaderboard` command. This holds commands viewing all the server's different leaderboards.
 */
public class LeaderboardCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param pointsService The {@link QOTWPointsService}
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param dbHelper An object managing databse operations
	 * @param dbActions A utility object providing various operations on the main database
	 */
	public LeaderboardCommand(QOTWPointsService pointsService, ExecutorService asyncPool, DbHelper dbHelper, DbActions dbActions) {
		setSlashCommandData(Commands.slash("leaderboard", "Command for all leaderboards.")
				.setGuildOnly(true)
		);
		addSubcommands(
				new QOTWLeaderboardSubcommand(pointsService, asyncPool, dbActions.getDataSource()),
				new ThanksLeaderboardSubcommand(asyncPool, dbActions),
				new ExperienceLeaderboardSubcommand(dbHelper));
	}
}
