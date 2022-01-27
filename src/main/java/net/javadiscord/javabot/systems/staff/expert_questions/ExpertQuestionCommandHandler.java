package net.javadiscord.javabot.systems.staff.expert_questions;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.staff.expert_questions.subcommands.AddExpertQuestionSubcommand;
import net.javadiscord.javabot.systems.staff.expert_questions.subcommands.ListExpertQuestionsSubcommand;
import net.javadiscord.javabot.systems.staff.expert_questions.subcommands.RemoveExpertQuestionSubcommand;

/**
 * Handler class for all expert question commands.
 */
public class ExpertQuestionCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public ExpertQuestionCommandHandler() {
		addSubcommand("add", new AddExpertQuestionSubcommand());
		addSubcommand("remove", new RemoveExpertQuestionSubcommand());
		addSubcommand("list", new ListExpertQuestionsSubcommand());
	}
}
