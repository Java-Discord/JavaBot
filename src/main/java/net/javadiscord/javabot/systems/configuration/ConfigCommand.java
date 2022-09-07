package net.javadiscord.javabot.systems.configuration;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;

/**
 * The main command for interacting with the bot's configuration at runtime via
 * slash commands.
 */
public class ConfigCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public ConfigCommand(BotConfig botConfig) {
		setModerationSlashCommandData(Commands.slash("config", "Administrative Commands for managing the bot's configuration."));
		addSubcommands(new ExportConfigSubcommand(botConfig), new GetConfigSubcommand(botConfig), new SetConfigSubcommand(botConfig));
		requireUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}
}

