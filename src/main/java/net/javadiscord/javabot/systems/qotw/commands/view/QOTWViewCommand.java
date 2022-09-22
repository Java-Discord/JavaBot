package net.javadiscord.javabot.systems.qotw.commands.view;

import java.util.concurrent.ExecutorService;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;

/**
 * Represents the `/qotw-view` command.
 * It allows to view previous QOTWs and their answers.
 */
public class QOTWViewCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 * @param dbActions A utility object providing various operations on the main database
	 * @param questionQueueRepository Dao class that represents the QOTW_QUESTION SQL Table.
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param qotwSubmissionRepository Dao object that represents the QOTW_SUBMISSIONS SQL Table.
	 */
	public QOTWViewCommand(BotConfig botConfig, DbHelper dbHelper, DbActions dbActions, ExecutorService asyncPool, QuestionQueueRepository questionQueueRepository, QOTWSubmissionRepository qotwSubmissionRepository) {
		setSlashCommandData(Commands.slash("qotw-view", "Query 'Questions of the Week' and their answers")
				.setDefaultPermissions(DefaultMemberPermissions.ENABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new QOTWQuerySubcommand(asyncPool, questionQueueRepository), new QOTWListAnswersSubcommand(botConfig, asyncPool, qotwSubmissionRepository), new QOTWViewAnswerSubcommand(botConfig, asyncPool, qotwSubmissionRepository));

	}
}
