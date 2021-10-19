package com.javadiscord.javabot.service.jam;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.service.jam.subcommands.JamInfoSubcommand;
import com.javadiscord.javabot.service.jam.subcommands.JamSubmitSubcommand;

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
