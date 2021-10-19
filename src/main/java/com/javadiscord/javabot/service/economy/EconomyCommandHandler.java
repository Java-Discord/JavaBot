package com.javadiscord.javabot.service.economy;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.service.economy.subcommands.AccountSubcommand;
import com.javadiscord.javabot.service.economy.subcommands.PreferencesSubcommand;
import com.javadiscord.javabot.service.economy.subcommands.SendSubcommand;

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
