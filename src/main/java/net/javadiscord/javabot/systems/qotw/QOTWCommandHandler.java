package net.javadiscord.javabot.systems.qotw;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.qotw.subcommands.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.RemoveQuestionSubcommand;

import java.util.Map;

public class QOTWCommandHandler extends DelegatingCommandHandler {
	public QOTWCommandHandler() {
		this.addSubcommandGroup("questions-queue", new DelegatingCommandHandler(Map.of(
				"list", new ListQuestionsSubcommand(),
				"add", new AddQuestionSubcommand(),
				"remove", new RemoveQuestionSubcommand()
		)));
	}
}
