package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.UnknownPropertyException;

/**
 * Subcommand that allows staff-members to get a single property variable from the guild config.
 */
public class GetSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var propertyOption = event.getOption("property");
		if (propertyOption == null) {
			return Responses.warning(event, "Missing required property argument.");
		}
		String property = propertyOption.getAsString().trim();
		try {
			Object value = Bot.config.get(event.getGuild()).resolve(property);
			return Responses.info(event, "Configuration Property", String.format("The value of the property `%s` is:\n```\n%s\n```", property, value));
		} catch (UnknownPropertyException e) {
			return Responses.warning(event, "Unknown Property", "The property `" + property + "` could not be found.");
		}
	}
}
