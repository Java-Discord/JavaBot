package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nonnull;

/**
 * Subcommand that allows staff-members to get a single property variable from the guild config.
 */
public class GetSubcommand extends ConfigSubcommand {
	@Override
	public ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException {
		OptionMapping propertyOption = event.getOption("property");
		if (propertyOption == null) {
			return Responses.warning(event, "Missing required property argument.");
		}
		String property = propertyOption.getAsString().trim();
		Object value = config.resolve(property);
		return Responses.info(event, "Configuration Property", String.format("The value of the property `%s` is:\n```\n%s\n```", property, value));
	}
}
