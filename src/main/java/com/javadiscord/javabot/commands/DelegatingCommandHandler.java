package com.javadiscord.javabot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.Map;

/**
 * Abstract command handler which is useful for commands which consist of lots
 * of subcommands. A child class will supply a map of subcommand handlers, so
 * that this parent handler can do the logic of finding the right subcommand to
 * invoke depending on the event received.
 */
public class DelegatingCommandHandler implements SlashCommandHandler {
	private final Map<String, SlashCommandHandler> subcommandHandlers;

	public DelegatingCommandHandler(Map<String, SlashCommandHandler> subcommandHandlers) {
		this.subcommandHandlers = subcommandHandlers;
	}

	@Override
	public void handle(SlashCommandEvent event) {
		if (event.getSubcommandName() == null) {
			this.handleNonSubcommand(event);
		} else {
			SlashCommandHandler handler = this.subcommandHandlers.get(event.getSubcommandName());
			if (handler != null) {
				handler.handle(event);
			} else {
				event.getHook().setEphemeral(true);
				event.getHook().sendMessage("Unknown subcommand.").queue();
			}
		}
	}

	protected void handleNonSubcommand(SlashCommandEvent event) {
		event.getHook().setEphemeral(true);
		event.getHook().sendMessage("Please specify a subcommand.").queue();
	}
}
