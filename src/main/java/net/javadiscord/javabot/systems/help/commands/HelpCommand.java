package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpAccountSubcommand;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpGuidelinesCommand;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpPingCommand;

/**
 * Represents the `/help` command. This holds commands related to the help system.
 */
public class HelpCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public HelpCommand() {
		setSlashCommandData(Commands.slash("help", "Commands related to the help system.")
				.setGuildOnly(true)
		);
		addSubcommands(new HelpAccountSubcommand(), new HelpPingCommand(), new HelpGuidelinesCommand());
	}
}
