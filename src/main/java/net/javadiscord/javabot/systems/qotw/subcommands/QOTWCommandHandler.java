package net.javadiscord.javabot.systems.qotw.subcommands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.ClearSubCommand;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubCommand;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.SetSubCommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.RemoveQuestionSubcommand;

import java.util.Map;

public class QOTWCommandHandler extends DelegatingCommandHandler {
	public QOTWCommandHandler() {
		this.addSubcommandGroup(
				"questions-queue", new DelegatingCommandHandler(Map.of(
				"list", new ListQuestionsSubcommand(),
				"add", new AddQuestionSubcommand(),
				"remove", new RemoveQuestionSubcommand()
		)));
		this.addSubcommandGroup(
				"account", new DelegatingCommandHandler(Map.of(
				"increment", new IncrementSubCommand(),
				"clear", new ClearSubCommand(),
				"set", new SetSubCommand()
		)));
	}
}
