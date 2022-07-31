package net.javadiscord.javabot.systems.user_preferences.commands;

import com.dynxsty.dih4jda.interactions.commands.AutoCompletable;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.util.AutoCompleteUtils;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceManager;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * <h3>This class represents the /preferences command.</h3>
 */
public class UserPreferenceCommand extends SlashCommand implements AutoCompletable {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public UserPreferenceCommand() {
		setSlashCommandData(Commands.slash("preferences", "Allows you to set some preferences!")
				.addOption(OptionType.INTEGER, "preference", "The preference to set.", true, true)
				.addOption(OptionType.BOOLEAN, "state", "The state of the specified preference.", true)
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
		UserPreferenceManager manager = new UserPreferenceManager(Bot.getDataSource());
		if (manager.setOrCreate(event.getUser().getIdLong(), preference, state)) {
			Responses.info(event, "Preference Updated", "Successfully set `%s` to `%s`!", preference, state).queue();
		} else {
			Responses.error(event, "Could not update `%s` to `%s`.", preference, state).queue();
		}
	}

	private @NotNull List<Command.Choice> getPreferenceChoices(long userId) {
		List<Command.Choice> choices = new ArrayList<>(Preference.values().length);
		UserPreferenceManager manager = new UserPreferenceManager(Bot.getDataSource());
		for (Preference p : Preference.values()) {
			choices.add(new Command.Choice(String.format("%s (%s)", p, manager.getOrCreate(userId, p).isEnabled() ? "Enabled" : "Disabled"), p.ordinal()));
		}
		return choices;
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		event.replyChoices(AutoCompleteUtils.handleChoices(event, e -> getPreferenceChoices(e.getUser().getIdLong()))).queue();
	}
}
