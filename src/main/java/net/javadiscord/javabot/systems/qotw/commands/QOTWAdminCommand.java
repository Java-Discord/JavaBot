package net.javadiscord.javabot.systems.qotw.commands;

import java.util.Map;
import java.util.Set;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.javadiscord.javabot.systems.qotw.commands.qotw_points.IncrementPointsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.qotw_points.SetPointsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.RemoveQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.submissions.subcommands.MarkBestAnswerSubcommand;

/**
 * Represents the `/qotw-admin` command. This holds administrative commands for managing the Question of the Week.
 */
public class QOTWAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 * @param listQuestionsSubcommand /qotw-admin questions-queue list-questions
	 * @param addQuestionSubcommand /qotw-admin questions-queue add
	 * @param removeQuestionSubcommand /qotw-admin questions-queue remove
	 * @param incrementPointsSubcommand /qotw-admin account increment
	 * @param setPointsSubcommand /qotw-admin account set
	 * @param markBestAnswerSubcommand /qotw-admin submissions mark-best
	 */
	public QOTWAdminCommand(ListQuestionsSubcommand listQuestionsSubcommand, AddQuestionSubcommand addQuestionSubcommand, RemoveQuestionSubcommand removeQuestionSubcommand, IncrementPointsSubcommand incrementPointsSubcommand, SetPointsSubcommand setPointsSubcommand, MarkBestAnswerSubcommand markBestAnswerSubcommand) {
		setSlashCommandData(Commands.slash("qotw-admin", "Administrative tools for managing the Question of the Week.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommandGroups(Map.of(
				new SubcommandGroupData("questions-queue", "Commands for interacting with the set of QOTW questions that are in queue."), Set.of(listQuestionsSubcommand, addQuestionSubcommand, removeQuestionSubcommand),
				new SubcommandGroupData("account", "Commands for interaction with Users Question of the Week points."), Set.of(incrementPointsSubcommand, setPointsSubcommand),
				new SubcommandGroupData("submissions", "Commands for managing QOTW Submissions."), Set.of(markBestAnswerSubcommand)
		));
	}
}
