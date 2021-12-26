package net.javadiscord.javabot.systems.economy;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.economy.subcommands.AccountSubcommand;
import net.javadiscord.javabot.systems.economy.subcommands.PreferencesSubcommand;
import net.javadiscord.javabot.systems.economy.subcommands.SendSubcommand;

import java.util.Map;

public class EconomyCommandHandler extends DelegatingCommandHandler {
	public EconomyCommandHandler() {
		super(Map.of(
				"account", new AccountSubcommand(),
				"send", new SendSubcommand(),
				"preferences", new PreferencesSubcommand()
		));
	}
}
