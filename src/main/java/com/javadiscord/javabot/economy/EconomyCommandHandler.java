package com.javadiscord.javabot.economy;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.economy.subcommands.AccountSubcommand;
import com.javadiscord.javabot.economy.subcommands.PreferencesSubcommand;
import com.javadiscord.javabot.economy.subcommands.SendSubcommand;

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
