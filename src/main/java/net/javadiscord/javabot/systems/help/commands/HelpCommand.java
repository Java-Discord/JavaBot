package net.javadiscord.javabot.systems.help.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;

/**
 * Represents the `/help` command. This holds commands related to the help system.
 */
public class HelpCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param helpAccountSubcommand /help account
	 * @param helpPingSubcommand /help ping
	 * @param helpGuidelinesSubcommand /help guidelines
	 */
	public HelpCommand(BotConfig botConfig, HelpAccountSubcommand helpAccountSubcommand, HelpPingSubcommand helpPingSubcommand, HelpGuidelinesSubcommand helpGuidelinesSubcommand) {
		setCommandData(Commands.slash("help", "Commands related to the help system.")
				.setGuildOnly(true)
		);
		addSubcommands(helpAccountSubcommand, helpPingSubcommand, helpGuidelinesSubcommand);
	}
}
