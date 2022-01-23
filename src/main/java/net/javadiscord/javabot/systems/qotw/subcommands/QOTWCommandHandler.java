package net.javadiscord.javabot.systems.qotw.subcommands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.ClearSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.SetSubCommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.RemoveQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.submission.AcceptSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.submission.DeclineSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.submission.DeleteSubcommand;

import java.util.Map;

/**
 * Handler class for all QOTW Commands.
 */
public class QOTWCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands and subcommand groups
	 * {@link DelegatingCommandHandler#addSubcommand}
	 * {@link DelegatingCommandHandler#addSubcommandGroup}.
	 */
	public QOTWCommandHandler() {
		this.addSubcommandGroup(
				"questions-queue", new DelegatingCommandHandler(Map.of(
						"list", new ListQuestionsSubcommand(),
						"add", new AddQuestionSubcommand(),
						"remove", new RemoveQuestionSubcommand()
				)));
		this.addSubcommandGroup(
				"account", new DelegatingCommandHandler(Map.of(
						"increment", new IncrementSubcommand(),
						"clear", new ClearSubcommand(),
						"set", new SetSubCommand()
				)));
		this.addSubcommandGroup(
				"submission", new DelegatingCommandHandler(Map.of(
						"accept", new AcceptSubcommand(),
						"decline", new DeclineSubcommand(),
						"delete", new DeleteSubcommand()
				))
		);
	}
}
