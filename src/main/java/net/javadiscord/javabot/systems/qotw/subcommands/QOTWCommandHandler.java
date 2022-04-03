package net.javadiscord.javabot.systems.qotw.subcommands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.interfaces.Autocompletable;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.ClearSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.IncrementSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.qotw_points.SetSubCommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.AddQuestionSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.ListQuestionsSubcommand;
import net.javadiscord.javabot.systems.qotw.subcommands.questions_queue.RemoveQuestionSubcommand;

import java.util.Map;

/**
 * Handler class for all QOTW Commands.
 */
public class QOTWCommandHandler extends DelegatingCommandHandler implements Autocompletable {
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
	}

	@Override
	public AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
		return switch (event.getSubcommandName()) {
			case "remove" -> RemoveQuestionSubcommand.replyQuestions(event);
			default -> event.replyChoices();
		};
	}
}
