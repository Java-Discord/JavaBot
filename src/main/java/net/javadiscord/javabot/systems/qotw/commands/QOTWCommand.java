package net.javadiscord.javabot.systems.qotw.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.javadiscord.javabot.systems.qotw.commands.qotw_points.IncrementPointsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.qotw_points.SetPointsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.commands.questions_queue.RemoveQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.submissions.subcommands.MarkBestAnswerSubcommand;

import java.util.Map;
import java.util.Set;

/**
 * Represents the `/qotw` command. This holds administrative commands for managing the Question of the Week.
 */
public class QOTWCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 */
	public QOTWCommand() {
		setSlashCommandData(Commands.slash("qotw", "Administrative tools for managing the Question of the Week.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommandGroups(Map.of(
				new SubcommandGroupData("questions-queue", "Commands for interacting with the set of QOTW questions that are in queue."), Set.of(new ListQuestionsSubcommand(), new AddQuestionSubcommand(), new RemoveQuestionSubcommand()),
				new SubcommandGroupData("account", "Commands for interaction with Users Question of the Week points."), Set.of(new IncrementPointsSubcommand(), new SetPointsSubcommand()),
				new SubcommandGroupData("submissions", "Commands for managing QOTW Submissions."), Set.of(new MarkBestAnswerSubcommand())
		));
	}
}
