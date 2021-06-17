package com.javadiscord.javabot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

/**
 * Implement this interface to declare that your class handles certain slash
 * commands.
 * <p>
 *     <strong>All implementing classes should have a public, no-args
 *     constructor.</strong>
 * </p>
 */
public interface SlashCommandHandler {
	void handle(SlashCommandEvent event);
}
