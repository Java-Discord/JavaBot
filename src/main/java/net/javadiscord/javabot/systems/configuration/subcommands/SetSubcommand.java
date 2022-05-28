package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.UnknownPropertyException;

/**
 * Subcommand that allows staff-members to edit the bot's configuration.
 */
public class SetSubcommand extends ConfigSubcommand {
	@Override
	public ReplyCallbackAction handleConfigSubcommand(SlashCommandInteractionEvent event, GuildConfig config) throws UnknownPropertyException {
		OptionMapping propertyOption = event.getOption("property");
		OptionMapping valueOption = event.getOption("value");
		if (propertyOption == null || valueOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}
		String property = propertyOption.getAsString().trim();
		String valueString = valueOption.getAsString().trim();
		Bot.config.get(event.getGuild()).set(property, valueString);
		return Responses.success(event, "Configuration Updated", String.format("The property `%s` has been set to `%s`.", property, valueString));
	}
}
