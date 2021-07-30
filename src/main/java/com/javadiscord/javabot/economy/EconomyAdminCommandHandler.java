package com.javadiscord.javabot.economy;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.economy.subcommands.admin.GiveSubcommand;

import java.util.Map;

public class EconomyAdminCommandHandler extends DelegatingCommandHandler {
	public EconomyAdminCommandHandler() {
		super(Map.of(
				"give", new GiveSubcommand()
		));
	}
}
