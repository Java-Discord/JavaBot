package net.javadiscord.javabot.systems.expert_questions;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.expert_questions.subcommands.AddSubcommand;
import net.javadiscord.javabot.systems.expert_questions.subcommands.ListSubCommand;
import net.javadiscord.javabot.systems.expert_questions.subcommands.RemoveSubCommand;

/**
 * Handler class for all expert question commands.
 */
public class ExpertQuestionCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public ExpertQuestionCommandHandler() {
		addSubcommand("add", new AddSubcommand());
		addSubcommand("remove", new RemoveSubCommand());
		addSubcommand("list", new ListSubCommand());
	}
}
