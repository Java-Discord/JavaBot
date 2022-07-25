package net.javadiscord.javabot.systems.configuration;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nonnull;

/**
 * Subcommand that allows staff-members to edit the bot's configuration.
 */
public class SetConfigSubcommand extends ConfigSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public SetConfigSubcommand() {
		setSubcommandData(new SubcommandData("set", "Sets the value of a configuration property.")
				.addOption(OptionType.STRING, "property", "The name of a property.", true)
				.addOption(OptionType.STRING, "value", "The value to set for the property.", true)
		);
		requireUsers(Bot.config.getSystems().getAdminConfig().getAdminUsers());
	}

	@Override
	public ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException {
		OptionMapping propertyOption = event.getOption("property");
		OptionMapping valueOption = event.getOption("value");
		if (propertyOption == null || valueOption == null) {
			return Responses.replyMissingArguments(event);
		}
		String property = propertyOption.getAsString().trim();
		String valueString = valueOption.getAsString().trim();
		Bot.config.get(event.getGuild()).set(property, valueString);
		return Responses.success(event, "Configuration Updated", "The property `%s` has been set to `%s`.", property, valueString);
	}
}
