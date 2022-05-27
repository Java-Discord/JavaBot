package net.javadiscord.javabot.systems.configuration;

import net.javadiscord.javabot.systems.configuration.subcommands.GetSubcommand;
import net.javadiscord.javabot.systems.configuration.subcommands.ListSubcommand;
import net.javadiscord.javabot.systems.configuration.subcommands.SetSubcommand;

/**
 * The main command for interacting with the bot's configuration at runtime via
 * slash commands.
 */
public class ConfigCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public ConfigCommandHandler() {
		addSubcommand("list", new ListSubcommand());
		addSubcommand("get", new GetSubcommand());
		addSubcommand("set", new SetSubcommand());
	}
}

