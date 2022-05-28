package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpAccountSubcommand;

/**
 * Handler class for all Help Commands.
 */
public class HelpCommand extends SlashCommand {
	public HelpCommand() {
		setCommandData(Commands.slash("help", "Commands for managing your Help Account."));
		setSubcommands(new HelpAccountSubcommand());
	}
}
