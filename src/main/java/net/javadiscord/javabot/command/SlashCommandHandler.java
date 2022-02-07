package net.javadiscord.javabot.command;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

/**
 * Implement this interface to declare that your class handles certain slash
 * commands.
 * <p>
 * <strong>All implementing classes should have a public, no-args
 * constructor.</strong>
 * </p>
 * <p>
 * To enable this handler for a particular slash command, navigate to that
 * command's entry in the corresponding YAML-file, and add the following property:
 * <pre><code>
 * handler: com.javadiscord.javabot.commands.MyFullHandlerClassName
 *     </code></pre>
 * </p>
 */
public interface SlashCommandHandler {
	ReplyAction handle(SlashCommandEvent event) throws ResponseException;
}
