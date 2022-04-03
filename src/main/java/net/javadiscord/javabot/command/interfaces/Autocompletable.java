package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;

/**
 * Interface that handles <a href="https://discord.com/developers/docs/interactions/application-commands#autocomplete">Discord's Autocomplete functionality</a>.
 * <p>
 * To enable this handler for a particular slash command option, navigate to that
 * option's entry in the corresponding YAML-file, and add the following property:
 * <pre><code>
 * autocomplete: true
 *     </code></pre>
 * </p>
 */
public interface Autocompletable {
	AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event);
}