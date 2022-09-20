package net.javadiscord.javabot.systems.help.commands;

import java.util.concurrent.ScheduledExecutorService;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;

/**
 * A simple command that can be used inside reserved help channels to immediately unreserve them,
 * instead of waiting for a timeout.
 * An alias to /unreserve
 */
public class CloseCommand extends UnreserveCommand {

	/**
	 * The constructor of this class, which sets the corresponding
	 * {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param botConfig The main configuration of the bot
	 * @param dbActions A utility object providing various operations on the main database
	 */
	public CloseCommand(BotConfig botConfig, ScheduledExecutorService asyncPool, DbActions dbActions) {
		super(botConfig, asyncPool, dbActions);
		setSlashCommandData(
				Commands.slash("close", "Unreserves this help channel so that others can use it.")
						.setGuildOnly(true).addOption(OptionType.STRING, "reason",
								"The reason why you're unreserving this channel", false));
	}
}
