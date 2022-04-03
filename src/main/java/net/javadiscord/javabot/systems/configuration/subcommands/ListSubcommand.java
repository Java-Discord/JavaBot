package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

import java.io.File;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ListSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		return event.deferReply()
				.addFile(new File("config/" + event.getGuild().getId() + ".json"));
	}
}
