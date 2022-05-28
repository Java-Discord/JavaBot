package net.javadiscord.javabot.systems.configuration;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.systems.configuration.subcommands.GetSubcommand;
import net.javadiscord.javabot.systems.configuration.subcommands.ListSubcommand;
import net.javadiscord.javabot.systems.configuration.subcommands.SetSubcommand;

/**
 * The main command for interacting with the bot's configuration at runtime via
 * slash commands.
 */
public class ConfigCommand extends SlashCommand {

	public ConfigCommand() {
		setCommandData(Commands.slash("config", "Administrative Commands for managing the bot's configuration.")
				// TODO: Implement App Permissions V2 once JDA releases them
				.setDefaultEnabled(false));
		setSubcommands(new ListSubcommand(), new GetSubcommand(), new SetSubcommand());
	}
}

