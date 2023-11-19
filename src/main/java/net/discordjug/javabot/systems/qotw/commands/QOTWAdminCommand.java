package net.discordjug.javabot.systems.qotw.commands;

import net.discordjug.javabot.systems.qotw.commands.qotw_points.DecrementPointsSubcommand;
import net.discordjug.javabot.systems.qotw.commands.qotw_points.IncrementPointsSubcommand;
import net.discordjug.javabot.systems.qotw.commands.questions_queue.AddQuestionSubcommand;
import net.discordjug.javabot.systems.qotw.commands.questions_queue.ListQuestionsSubcommand;
import net.discordjug.javabot.systems.qotw.commands.questions_queue.RemoveQuestionSubcommand;
import net.discordjug.javabot.systems.qotw.submissions.subcommands.QOTWReviewSubcommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Represents the `/qotw-admin` command. This holds administrative commands for managing the Question of the Week.
 */
public class QOTWAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 *
	 * @param listQuestionsSubcommand   /qotw-admin questions-queue list-questions
	 * @param addQuestionSubcommand     /qotw-admin questions-queue add
	 * @param removeQuestionSubcommand  /qotw-admin questions-queue remove
	 * @param incrementPointsSubcommand /qotw-admin account increment
	 * @param decrementPointsSubcommand /qotw-admin account decrement
	 * @param reviewSubcommand          /qotw-admin submissions review
	 */
	public QOTWAdminCommand(ListQuestionsSubcommand listQuestionsSubcommand, AddQuestionSubcommand addQuestionSubcommand, RemoveQuestionSubcommand removeQuestionSubcommand, IncrementPointsSubcommand incrementPointsSubcommand, DecrementPointsSubcommand decrementPointsSubcommand, QOTWReviewSubcommand reviewSubcommand) {
		setCommandData(Commands.slash("qotw-admin", "Administrative tools for managing the Question of the Week.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(reviewSubcommand);
		addSubcommandGroups(
				SubcommandGroup.of(new SubcommandGroupData("questions-queue", "Commands for interacting with the set of QOTW questions that are in queue."), listQuestionsSubcommand, addQuestionSubcommand, removeQuestionSubcommand),
				SubcommandGroup.of(new SubcommandGroupData("account", "Commands for interaction with Users Question of the Week points."), incrementPointsSubcommand, decrementPointsSubcommand),
				SubcommandGroup.of(new SubcommandGroupData("submissions", "Commands for managing QOTW Submissions."), reviewSubcommand)
		);
	}
}
