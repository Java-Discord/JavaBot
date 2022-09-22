package net.javadiscord.javabot.systems.help.commands;

import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;

/**
 * Represents the `/help` command. This holds commands related to the help system.
 */
public class HelpCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param botConfig The main configuration of the bot
	 * @param dataSource A factory for connections to the main database
	 * @param dbActions A service object responsible for various operations on the main database
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 */
	public HelpCommand(DataSource dataSource, BotConfig botConfig, ScheduledExecutorService asyncPool, DbActions dbActions, HelpExperienceService helpExperienceService) {
		setSlashCommandData(Commands.slash("help", "Commands related to the help system.")
				.setGuildOnly(true)
		);
		addSubcommands(new HelpAccountSubcommand(dbActions, helpExperienceService), new HelpPingSubcommand(botConfig, asyncPool, helpExperienceService, dbActions), new HelpGuidelinesSubcommand(botConfig));
	}
}
