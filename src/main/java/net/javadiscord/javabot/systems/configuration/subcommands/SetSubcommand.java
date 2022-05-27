package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.UnknownPropertyException;

/**
 * Subcommand that allows staff-members to edit the bot's configuration.
 */
public class SetSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var propertyOption = event.getOption("property");
		var valueOption = event.getOption("value");
		if (propertyOption == null || valueOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}
		String property = propertyOption.getAsString().trim();
		String valueString = valueOption.getAsString().trim();
		try {
			Bot.config.get(event.getGuild()).set(property, valueString);
			return Responses.success(event, "Configuration Updated", String.format("The property `%s` has been set to `%s`.", property, valueString));
		} catch (UnknownPropertyException e) {
			return Responses.warning(event, "Unknown Property", "The property `" + property + "` could not be found.");
		}
	}
}
