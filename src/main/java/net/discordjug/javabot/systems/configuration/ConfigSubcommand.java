package net.discordjug.javabot.systems.configuration;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.UnknownPropertyException;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * An abstraction of {@link xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand} which handles all
 * config-related commands.
 */
public abstract class ConfigSubcommand extends SlashCommand.Subcommand {
	/**
	 * The main configuration of the bot.
	 */
	protected final BotConfig botConfig;

	public ConfigSubcommand(BotConfig botConfig) {
		this.botConfig = botConfig;
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasAdminRole(botConfig, event.getMember())) {
			Responses.replyAdminOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		try {
			handleConfigSubcommand(event, botConfig.get(event.getGuild())).queue();
		} catch (UnknownPropertyException e) {
			Responses.warnin(event, "Unknown Property", "The provided property could not be found.")
					.queue();
		}
	}

	protected abstract InteractionCallbackAction<?> handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException;

	/**
	 * autocompletes a property.
	 * @param event the {@link CommandAutoCompleteInteractionEvent} used for sending the autocomplete information
	 * @param partialText the entered text
	 */
	protected void handlePropertyAutocomplete(CommandAutoCompleteInteractionEvent event, String partialText) {
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
