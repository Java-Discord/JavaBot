package net.javadiscord.javabot.systems.jam;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.jam.subcommands.JamInfoSubcommand;
import net.javadiscord.javabot.systems.jam.subcommands.JamSubmitSubcommand;

import java.util.Map;

/**
 * Main command handler for Jam commands.
 */
public class JamCommandHandler extends DelegatingCommandHandler {
	public JamCommandHandler() {
		super(Map.of(
				"info", new JamInfoSubcommand(),
				"submit", new JamSubmitSubcommand()
		));
	}
}
