package net.javadiscord.javabot.systems.economy;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.economy.subcommands.AccountSubcommand;
import net.javadiscord.javabot.systems.economy.subcommands.PreferencesSubcommand;
import net.javadiscord.javabot.systems.economy.subcommands.SendSubcommand;

import java.util.Map;

/**
 * Handler class for all economy commands.
 */
public class EconomyCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public EconomyCommandHandler() {
		super(Map.of(
				"account", new AccountSubcommand(),
				"send", new SendSubcommand(),
				"preferences", new PreferencesSubcommand()
		));
	}
}
