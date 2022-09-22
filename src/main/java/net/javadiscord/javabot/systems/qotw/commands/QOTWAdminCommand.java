package net.javadiscord.javabot.systems.qotw.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.systems.qotw.commands.qotw_points.IncrementPointsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.qotw_points.SetPointsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.RemoveQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.submissions.dao.QOTWSubmissionRepository;
import net.javadiscord.javabot.systems.qotw.submissions.subcommands.MarkBestAnswerSubcommand;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Represents the `/qotw-admin` command. This holds administrative commands for managing the Question of the Week.
 */
public class QOTWAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 * @param pointsService The {@link QOTWPointsService}
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 * @param qotwPointsRepository Dao object that represents the QOTW_POINTS SQL Table.
	 * @param questionQueueRepository Dao class that represents the QOTW_QUESTION SQL Table.
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param qotwSubmissionRepository Dao object that represents the QOTW_SUBMISSIONS SQL Table.
	 */
	public QOTWAdminCommand(QOTWPointsService pointsService, NotificationService notificationService, BotConfig botConfig, DbHelper dbHelper, QuestionPointsRepository qotwPointsRepository, QuestionQueueRepository questionQueueRepository, ExecutorService asyncPool, QOTWSubmissionRepository qotwSubmissionRepository) {
		setSlashCommandData(Commands.slash("qotw-admin", "Administrative tools for managing the Question of the Week.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommandGroups(Map.of(
				new SubcommandGroupData("questions-queue", "Commands for interacting with the set of QOTW questions that are in queue."), Set.of(new ListQuestionsSubcommand(questionQueueRepository, asyncPool), new AddQuestionSubcommand(questionQueueRepository, asyncPool), new RemoveQuestionSubcommand(questionQueueRepository)),
				new SubcommandGroupData("account", "Commands for interaction with Users Question of the Week points."), Set.of(new IncrementPointsSubcommand(pointsService, notificationService), new SetPointsSubcommand(pointsService, dbHelper.getDataSource(), qotwPointsRepository)),
				new SubcommandGroupData("submissions", "Commands for managing QOTW Submissions."), Set.of(new MarkBestAnswerSubcommand(pointsService, notificationService, botConfig, asyncPool, qotwSubmissionRepository))
		));
	}
}
