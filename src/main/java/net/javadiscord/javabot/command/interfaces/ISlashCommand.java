package net.javadiscord.javabot.command.interfaces;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.command.ResponseException;

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
public interface ISlashCommand {
	InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException;
}
