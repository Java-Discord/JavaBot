package net.javadiscord.javabot.systems.jam;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.jam.subcommands.JamInfoSubcommand;
import net.javadiscord.javabot.systems.jam.subcommands.JamSubmitSubcommand;

/**
 * Handler class for all Jam commands.
 */
public class JamCommand extends SlashCommand {

	public JamCommand() {
		setSlashCommandData(Commands.slash("jam", "Interact with Java Jam functionality.")
				.setGuildOnly(true)
		);
		addSubcommands(new JamInfoSubcommand(), new JamSubmitSubcommand());
	}
}
