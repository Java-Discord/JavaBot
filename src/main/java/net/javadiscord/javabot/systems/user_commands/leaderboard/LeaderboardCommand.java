package net.javadiscord.javabot.systems.user_commands.leaderboard;

import java.util.concurrent.ExecutorService;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;

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
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table.
	 * @param helpTransactionRepository Dao object that represents the HELP_TRANSACTIONS SQL Table.
	 * @param qotwPointsRepository Dao object that represents the QOTW_POINTS SQL Table.
	 */
	public LeaderboardCommand(QOTWPointsService pointsService, ExecutorService asyncPool, DbHelper dbHelper, DbActions dbActions, HelpAccountRepository helpAccountRepository, HelpTransactionRepository helpTransactionRepository, QuestionPointsRepository qotwPointsRepository) {
		setCommandData(Commands.slash("leaderboard", "Command for all leaderboards.")
				.setGuildOnly(true)
		);
		addSubcommands(
				new QOTWLeaderboardSubcommand(pointsService, asyncPool, qotwPointsRepository),
				new ThanksLeaderboardSubcommand(asyncPool, dbActions),
				new ExperienceLeaderboardSubcommand(helpAccountRepository, asyncPool, helpTransactionRepository));
	}
}
