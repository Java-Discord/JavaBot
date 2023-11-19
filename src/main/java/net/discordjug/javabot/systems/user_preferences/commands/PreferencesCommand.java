package net.discordjug.javabot.systems.user_preferences.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * <h3>This class represents the /preferences command.</h3>
 */
public class PreferencesCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param preferencesListSubcommand /preferences list
	 * @param preferencesSetSubcommand /preferences set
	 */
	public PreferencesCommand(PreferencesListSubcommand preferencesListSubcommand, PreferencesSetSubcommand preferencesSetSubcommand) {
		setCommandData(Commands.slash("preferences", "Contains commands for managing user preferences.")
				.setGuildOnly(true)
		);
		addSubcommands(preferencesListSubcommand, preferencesSetSubcommand);
	}
}
