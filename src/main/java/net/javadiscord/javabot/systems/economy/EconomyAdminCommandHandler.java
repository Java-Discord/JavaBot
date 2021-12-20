package net.javadiscord.javabot.systems.economy;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.economy.subcommands.admin.GiveSubcommand;

import java.util.Map;

public class EconomyAdminCommandHandler extends DelegatingCommandHandler {
	public EconomyAdminCommandHandler() {
		super(Map.of(
				"give", new GiveSubcommand()
		));
	}
}
