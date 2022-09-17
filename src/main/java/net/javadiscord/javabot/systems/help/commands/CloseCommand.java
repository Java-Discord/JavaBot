package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * A simple command that can be used inside reserved help channels to immediately unreserve them,
 * instead of waiting for a timeout.
 */
public class CloseCommand extends UnreserveCommand {

	/**
	 * The constructor of this class, which sets the corresponding
	 * {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public CloseCommand() {
		setSlashCommandData(
				Commands.slash("close", "Unreserves this help channel so that others can use it.")
						.setGuildOnly(true).addOption(OptionType.STRING, "reason",
								"The reason why you're unreserving this channel", false));
	}
}
