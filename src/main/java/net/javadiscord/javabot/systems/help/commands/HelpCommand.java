package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.help.commands.subcommands.HelpAccountSubcommand;

/**
 * Represents the `/help` command. This holds commands for a user's Help Account.
 */
public class HelpCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public HelpCommand() {
		setSlashCommandData(Commands.slash("help", "Commands for managing your Help Account.")
				.setGuildOnly(true)
		);
		addSubcommands(new HelpAccountSubcommand());
	}
}
