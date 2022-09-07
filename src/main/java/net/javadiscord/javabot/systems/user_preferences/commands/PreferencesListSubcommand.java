package net.javadiscord.javabot.systems.user_preferences.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * <h3>This class represents the /preferences list command.</h3>
 */
public class PreferencesListSubcommand extends SlashCommand.Subcommand {
	private final UserPreferenceService service;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param service The {@link UserPreferenceService}
	 */
	public PreferencesListSubcommand(UserPreferenceService service) {
		this.service = service;
		setSubcommandData(new SubcommandData("list", "Shows all your preferences."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		String preferences = Arrays.stream(Preference.values())
				.map(p -> String.format("`%s` %s", service.getOrCreate(event.getUser().getIdLong(), p).isEnabled() ? "\uD83D\uDFE2" : "\uD83D\uDD34", p))
				.collect(Collectors.joining("\n"));
		Responses.info(event, String.format("%s's Preferences", event.getUser().getName()), preferences).queue();
	}
}
