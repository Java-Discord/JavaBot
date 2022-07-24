package net.javadiscord.javabot.systems.staff_commands.embeds;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;

/**
 * Represents the `/embed` command. This holds administrative commands for creating and editing embed messages.
 */
public class EmbedCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public EmbedCommand() {
		setModerationSlashCommandData(Commands.slash("embed", "Administrative commands for creating and editing embed messages."));
		addSubcommands(new CreateEmbedSubcommand(), new EditEmbedSubcommand(), new AddEmbedFieldSubcommand(), new RemoveEmbedFieldSubcommand());
	}
}