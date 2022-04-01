package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;

/**
 * Interface that handles Discord's Autocomplete functionality.
 * <p>
 * To enable this handler for a particular slash command option, navigate to that
 * option's entry in the corresponding YAML-file, and add the following property:
 * <pre><code>
 * autocomplete: true
 *     </code></pre>
 * </p>
 */
public interface Autocomplete {
	AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event);
}