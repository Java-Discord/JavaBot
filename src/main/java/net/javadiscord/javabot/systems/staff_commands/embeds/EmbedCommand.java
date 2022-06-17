package net.javadiscord.javabot.systems.staff_commands.embeds;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.staff_commands.embeds.subcommands.CreateEmbedSubcommand;
import net.javadiscord.javabot.systems.staff_commands.embeds.subcommands.EditEmbedSubcommand;

/**
 * Represents the `/embed` command. This holds administrative commands for creating and editing embed messages.
 */
public class EmbedCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public EmbedCommand() {
		setSlashCommandData(Commands.slash("embed", "Administrative commands for creating and editing embed messages.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new CreateEmbedSubcommand(), new EditEmbedSubcommand());
	}
}