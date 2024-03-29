package net.discordjug.javabot.systems.configuration;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.UnknownPropertyException;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

import javax.annotation.Nonnull;

/**
 * Subcommand that allows staff-members to get a single property variable from the guild config.
 */
public class GetConfigSubcommand extends ConfigSubcommand implements AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public GetConfigSubcommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(new SubcommandData("get", "Get the current value of a configuration property.")
				.addOption(OptionType.STRING, "property", "The name of a property.", true, true)
		);
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

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		if (target.getName().equals("property")) {
			String partialText = target.getValue();
			handlePropertyAutocomplete(event, partialText);
		}
	}
}
