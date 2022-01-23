package net.javadiscord.javabot.systems.expert_questions;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.expert_questions.subcommands.AddSubcommand;
import net.javadiscord.javabot.systems.expert_questions.subcommands.ListSubcommand;
import net.javadiscord.javabot.systems.expert_questions.subcommands.RemoveSubcommand;

/**
 * Handler class for all expert question commands.
 */
public class ExpertQuestionCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public ExpertQuestionCommandHandler() {
		addSubcommand("add", new AddSubcommand());
		addSubcommand("remove", new RemoveSubcommand());
		addSubcommand("list", new ListSubcommand());
	}
}
