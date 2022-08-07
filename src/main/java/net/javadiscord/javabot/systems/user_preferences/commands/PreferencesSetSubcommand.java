package net.javadiscord.javabot.systems.user_preferences.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * <h3>This class represents the /preferences set command.</h3>
 */
public class PreferencesSetSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public PreferencesSetSubcommand() {
		setSubcommandData(new SubcommandData("set", "Allows you to set your preferences!")
				.addOptions(
						new OptionData(OptionType.INTEGER, "preference", "The preference to set.", true)
								.addChoices(Arrays.stream(Preference.values()).map(this::toChoice).toList()),
						new OptionData(OptionType.BOOLEAN, "state", "The state of the specified preference.", true)
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
		boolean state = stateMapping.getAsBoolean();
		UserPreferenceService manager = new UserPreferenceService(Bot.getDataSource());
		if (manager.setOrCreate(event.getUser().getIdLong(), preference, state)) {
			Responses.info(event, "Preference Updated", "Successfully %s `%s`!", state ? "enabled" : "disabled", preference).queue();
		} else {
			Responses.error(event, "Could not %s `%s`.", state ? "enable" : "disable", preference).queue();
		}
	}

	@Contract("_ -> new")
	private Command.@NotNull Choice toChoice(@NotNull Preference preference) {
		return new Command.Choice(preference.toString(), String.valueOf(preference.ordinal()));
	}
}
