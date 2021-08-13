package com.javadiscord.javabot.qotw;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.qotw.subcommands.AddQuestionSubcommand;
import com.javadiscord.javabot.qotw.subcommands.ListQuestionsSubcommand;
import com.javadiscord.javabot.qotw.subcommands.RemoveQuestionSubcommand;

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
