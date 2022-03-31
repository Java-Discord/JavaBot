package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;

/**
 * Implement this interface to declare that your class handles Autocomplete functionality.
 */
public interface IAutocomplete {
	AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event);
}