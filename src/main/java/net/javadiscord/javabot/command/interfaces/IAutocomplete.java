package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;

/**
 * Implement this interface to declare that your class handles Autocomplete functionality.
 * <p>
 * <strong>All implementing classes should have a public, no-args
 * constructor.</strong>
 * </p>
 * <p>
 * To enable this handler for a particular slash command option, navigate to that
 * command's entry in the corresponding YAML-file, and add the following property:
 * <pre><code>
 * handler: com.javadiscord.javabot.commands.MyFullHandlerClassName
 *     </code></pre>
 * </p>
 */
public interface IAutocomplete {
	AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event);
}