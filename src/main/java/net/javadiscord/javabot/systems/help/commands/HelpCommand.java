package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.interactions.commands.build.Commands;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Represents the `/help` command. This holds commands related to the help system.
 */
public class HelpCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param helpAccountSubcommand /help account
	 * @param helpPingSubcommand /help ping
	 * @param helpGuidelinesSubcommand /help guidelines
	 * @param helpStatisticsSubcommand /help stats
	 */
	public HelpCommand(HelpAccountSubcommand helpAccountSubcommand, HelpPingSubcommand helpPingSubcommand, HelpGuidelinesSubcommand helpGuidelinesSubcommand, HelpStatisticsSubcommand helpStatisticsSubcommand) {
		setCommandData(Commands.slash("help", "Commands related to the help system.")
				.setGuildOnly(true)
		);
		addSubcommands(helpAccountSubcommand, helpPingSubcommand, helpGuidelinesSubcommand, helpStatisticsSubcommand);
	}
}
