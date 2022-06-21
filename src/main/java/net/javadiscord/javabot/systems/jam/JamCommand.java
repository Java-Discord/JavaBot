package net.javadiscord.javabot.systems.jam;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.jam.subcommands.JamInfoSubcommand;
import net.javadiscord.javabot.systems.jam.subcommands.JamSubmitSubcommand;

/**
 * Represents the `/jam` command. This holds commands for interacting with the Java Jam functionality.
 */
public class JamCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public JamCommand() {
		setSlashCommandData(Commands.slash("jam", "Interact with Java Jam functionality.")
				.setGuildOnly(true)
		);
		addSubcommands(new JamInfoSubcommand(), new JamSubmitSubcommand());
	}
}
