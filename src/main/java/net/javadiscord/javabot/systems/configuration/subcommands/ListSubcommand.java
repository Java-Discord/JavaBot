package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.io.File;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ListSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		return event.deferReply()
				.addFile(new File("config/" + event.getGuild().getId() + ".json"));
	}
}
