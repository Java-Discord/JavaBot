package net.discordjug.javabot.systems.configuration;

import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.UnknownPropertyException;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Subcommand that allows staff-members to edit the bot's configuration.
 */
@AutoDetectableComponentHandler("config-set")
public class SetConfigSubcommand extends ConfigSubcommand implements ModalHandler, AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public SetConfigSubcommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(new SubcommandData("set", "Sets the value of a configuration property.")
				.addOption(OptionType.STRING, "property", "The name of a property.", true, true)
				.addOption(OptionType.STRING, "value", "The value to set for the property.", false)
		);
	}

	@Override
	public InteractionCallbackAction<?> handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException {
		OptionMapping propertyOption = event.getOption("property");
		OptionMapping valueOption = event.getOption("value");
		if (propertyOption == null) {
			return Responses.replyMissingArguments(event);
		}
		String property = propertyOption.getAsString().trim();
		GuildConfig guildConfig = botConfig.get(event.getGuild());
		if (valueOption == null) {
			Object resolved = guildConfig.resolve(property);
			if (resolved == null) {
				return Responses.error(event, "Config `%s` not found", property);
			}
			return event.replyModal(
					Modal.create(ComponentIdBuilder.build("config-set", property), "Change configuration value")
					.addActionRow(TextInput.create("value", "new value", TextInputStyle.PARAGRAPH)
							.setValue(String.valueOf(resolved))
							.build())
				.build());
		}
		String valueString = valueOption.getAsString().trim();
		guildConfig.set(property, valueString);
		return Responses.success(event, "Configuration Updated", "The property `%s` has been set to `%s`.", property, valueString);
	}

	@Override
	public void handleModal(ModalInteractionEvent event, List<ModalMapping> values) {
		String[] id = ComponentIdBuilder.split(event.getModalId());
		String property = id[1];
		String valueString = event.getValue("value").getAsString();
		GuildConfig guildConfig = botConfig.get(event.getGuild());
		try {
			guildConfig.set(property, valueString);
			Responses.success(event, "Configuration Updated", "The property `%s` has been set to `%s`.", property, valueString).queue();
		} catch (UnknownPropertyException e) {
			Responses.error(event, "Property not found: %s", property).queue();
		}
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		if (target.getName().equals("property")) {
			String partialText = target.getValue();
			handlePropertyAutocomplete(event, partialText);
		}
	}
}
