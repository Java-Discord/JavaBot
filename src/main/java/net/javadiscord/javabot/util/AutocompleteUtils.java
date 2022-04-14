package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.List;

/**
 * Utility class for Autocomplete Interactions.
 */
public class AutocompleteUtils {

	private AutocompleteUtils() {}

	public static List<Command.Choice> filterChoices(CommandAutoCompleteInteractionEvent event, List<Command.Choice> choices) {
		return AutocompleteUtils.filterChoices(event.getFocusedOption().getValue(), choices);
	}

	public static List<Command.Choice> filterChoices(String filter, List<Command.Choice> choices) {
		choices.removeIf(choice -> !choice.getName().contains(filter));
		return choices;
	}
}
