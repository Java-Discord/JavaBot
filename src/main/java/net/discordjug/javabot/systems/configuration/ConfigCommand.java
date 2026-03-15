package net.discordjug.javabot.systems.configuration;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.systems.moderation.CommandModerationPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * The main command for interacting with the bot's configuration at runtime via
 * slash commands.
 */
public class ConfigCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param exportConfigSubcommand /config export
	 * @param getConfigSubcommand /config get
	 * @param setConfigSubcommand /config set
	 */
	public ConfigCommand(ExportConfigSubcommand exportConfigSubcommand, GetConfigSubcommand getConfigSubcommand, SetConfigSubcommand setConfigSubcommand) {
		setModerationSlashCommandData(Commands.slash("config", "Administrative Commands for managing the bot's configuration."));
		addSubcommands(exportConfigSubcommand, getConfigSubcommand, setConfigSubcommand);
	}
}

