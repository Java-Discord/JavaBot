package net.javadiscord.javabot.systems.jam;

import net.javadiscord.javabot.systems.jam.subcommands.JamInfoSubcommand;
import net.javadiscord.javabot.systems.jam.subcommands.JamSubmitSubcommand;

import java.util.Map;

/**
 * Handler class for all Jam commands.
 */
public class JamCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public JamCommandHandler() {
		super(Map.of(
				"info", new JamInfoSubcommand(),
				"submit", new JamSubmitSubcommand()
		));
	}
}
