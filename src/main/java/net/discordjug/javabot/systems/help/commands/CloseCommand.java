package net.discordjug.javabot.systems.help.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.h2db.DbActions;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * A simple command that can be used inside reserved help channels to immediately unreserve them,
 * instead of waiting for a timeout.
 * An alias to /unreserve.
 */
public class CloseCommand extends UnreserveCommand {

	/**
	 * The constructor of this class, which sets the corresponding
	 * {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param dbActions A utility object providing various operations on the main database
	 * @param helpTransactionRepository Dao object that represents the HELP_TRANSACTION SQL Table.
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table.
	 * @param preferenceService Service for user preferences
	 */
	public CloseCommand(BotConfig botConfig, DbActions dbActions, HelpTransactionRepository helpTransactionRepository, HelpAccountRepository helpAccountRepository, UserPreferenceService preferenceService) {
		super(botConfig, dbActions, helpTransactionRepository, helpAccountRepository, preferenceService);
		setCommandData(
				Commands.slash("close", "Unreserves this post marking your question/issue as resolved.")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, "reason", "The reason why you're unreserving this channel", false));
	}
}
