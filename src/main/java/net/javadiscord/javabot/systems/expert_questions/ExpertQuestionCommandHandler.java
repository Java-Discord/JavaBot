package net.javadiscord.javabot.systems.expert_questions;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.expert_questions.subcommands.AddSubCommand;
import net.javadiscord.javabot.systems.expert_questions.subcommands.ListSubCommand;
import net.javadiscord.javabot.systems.expert_questions.subcommands.RemoveSubCommand;

public class ExpertQuestionCommandHandler extends DelegatingCommandHandler {
	public ExpertQuestionCommandHandler() {
		addSubcommand("add", new AddSubCommand());
		addSubcommand("remove", new RemoveSubCommand());
		addSubcommand("list", new ListSubCommand());
	}
}
