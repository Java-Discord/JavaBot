package net.javadiscord.javabot.systems.user_preferences.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;

/**
 * <h3>This class represents the /preferences command.</h3>
 */
public class PreferencesCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param service The {@link UserPreferenceService}
	 */
	public PreferencesCommand(UserPreferenceService service) {
		setSlashCommandData(Commands.slash("preferences", "Contains commands for managing user preferences.")
				.setGuildOnly(true)
		);
		addSubcommands(new PreferencesListSubcommand(service), new PreferencesSetSubcommand(service));
	}
}
