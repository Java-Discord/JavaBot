package com.javadiscord.javabot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

/**
 * Implement this interface to declare that your class handles certain slash
 * commands.
 * <p>
 *     <strong>All implementing classes should have a public, no-args
 *     constructor.</strong>
 * </p>
 * <p>
 *     To enable this handler for a particular slash command, navigate to that
 *     command's entry in commands.yaml, and add the following property:
 *     <pre><code>
 * handler: com.javadiscord.javabot.commands.MyFullHandlerClassName
 *     </code></pre>
 * </p>
 */
public interface SlashCommandHandler {
	void handle(SlashCommandEvent event);
}
