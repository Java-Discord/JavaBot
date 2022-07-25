package net.javadiscord.javabot.systems.configuration.subcommands;

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
 * Subcommand that allows staff-members to get a single property variable from the guild config.
 */
public class GetConfigSubcommand extends ConfigSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public GetConfigSubcommand() {
		setSubcommandData(new SubcommandData("get", "Get the current value of a configuration property.")
				.addOption(OptionType.STRING, "property", "The name of a property.", true)
		);
		requireUsers(Bot.config.getSystems().getAdminConfig().getAdminUsers());
	}

	@Override
	public ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException {
		OptionMapping propertyOption = event.getOption("property");
		if (propertyOption == null) {
			return Responses.replyMissingArguments(event);
		}
		String property = propertyOption.getAsString().trim();
		Object value = config.resolve(property);
		return Responses.info(event, "Configuration Property", "The value of the property `%s` is:\n```\n%s\n```", property, value);
	}
}
