package net.javadiscord.javabot.systems.configuration;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Responses;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
			int lastDot = partialText.lastIndexOf('.');
			String parentPropertyName;
			String childPropertyName;
			if (lastDot == -1) {
				parentPropertyName = "";
				childPropertyName = partialText;
			} else {
				parentPropertyName = partialText.substring(0,lastDot);
				childPropertyName = partialText.substring(lastDot+1);
			}

			GuildConfig guildConfig = botConfig.get(event.getGuild());
			try {
				Object resolved;
				if(parentPropertyName.isEmpty()) {
					resolved = guildConfig;
				} else {
					resolved = guildConfig.resolve(parentPropertyName);
				}
				if (resolved == null || !resolved.getClass().getPackageName().startsWith(GuildConfig.class.getPackageName())) {
					event.replyChoices().queue();
					return;
				}
				List<Choice> choices = Arrays.stream(resolved.getClass().getDeclaredFields())
					.filter(f -> !Modifier.isTransient(f.getModifiers()))
					.map(Field::getName)
					.map(name -> parentPropertyName.isEmpty() ? name : parentPropertyName+"."+name)
					.map(name -> new Command.Choice(name, name))
					.collect(Collectors.toList());

				event.replyChoices(AutoCompleteUtils.filterChoices(childPropertyName, choices)).queue();
			} catch (UnknownPropertyException e) {
				event.replyChoices().queue();
			}
		}
	}
}
