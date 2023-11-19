package net.discordjug.javabot.systems.user_preferences.commands;

import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.discordjug.javabot.systems.user_preferences.model.Preference;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h3>This class represents the /preferences set command.</h3>
 */
public class PreferencesSetSubcommand extends SlashCommand.Subcommand implements AutoCompletable {
	private final UserPreferenceService service;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param service The {@link UserPreferenceService}
	 */
	public PreferencesSetSubcommand(UserPreferenceService service) {
		this.service = service;
		setCommandData(new SubcommandData("set", "Allows you to set your preferences!")
				.addOptions(
						new OptionData(OptionType.INTEGER, "preference", "The preference to set.", true)
								.addChoices(Arrays.stream(Preference.values()).map(this::toChoice).toList()),
						new OptionData(OptionType.STRING, "state", "The state/value of the specified preference.", true, true)
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping preferenceMapping = event.getOption("preference");
		OptionMapping stateMapping = event.getOption("state");
		if (preferenceMapping == null || stateMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		Preference preference = Preference.values()[preferenceMapping.getAsInt()];
		String state = stateMapping.getAsString();
		if (Arrays.stream(preference.getType().getAllowedChoices()).noneMatch(s -> s.equals(state))) {
			Responses.error(event, "`%s` is not allowed for this preference! Expected one of the following values:\n%s",
					state, String.join(", ", preference.getType().getAllowedChoices())
			).queue();
			return;
		}
		if (service.setOrCreate(event.getUser().getIdLong(), preference, state)) {
			Responses.info(event, "Preference Updated", "Successfully set `%s` to `%s`!", preference, state).queue();
		} else {
			Responses.error(event, "Could not set %s to `%s`.", preference, state).queue();
		}
	}

	@Contract("_ -> new")
	private Command.@NotNull Choice toChoice(@NotNull Preference preference) {
		return new Command.Choice(preference.toString(), preference.ordinal());
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		int preferenceInt = event.getOption("preference",-1, OptionMapping::getAsInt);
		if (preferenceInt > 0 && preferenceInt<Preference.values().length) {
			Preference preference = Preference.values()[preferenceInt];
			if (preference.getType().getDefaultChoices() != null && preference.getType().getDefaultChoices().length > 0) {
				event.replyChoices(AutoCompleteUtils.filterChoices(event, new ArrayList<>(List.of(preference.getType().getDefaultChoices())))).queue();
			}
		}
	}
}
