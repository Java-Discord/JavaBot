package net.discordjug.javabot.systems.staff_commands.embeds;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.systems.moderation.CommandModerationPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/embed` command. This holds administrative commands for creating and editing embed messages.
 */
public class EmbedCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param createEmbedSubcommand /embed create
	 * @param editEmbedSubcommand /embed edit
	 * @param addEmbedFieldSubcommand /embed add-field
	 * @param removeEmbedFieldSubcommand /embed remove-field
	 */
	public EmbedCommand(CreateEmbedSubcommand createEmbedSubcommand, EditEmbedSubcommand editEmbedSubcommand, AddEmbedFieldSubcommand addEmbedFieldSubcommand, RemoveEmbedFieldSubcommand removeEmbedFieldSubcommand) {
		setModerationSlashCommandData(Commands.slash("embed", "Administrative commands for creating and editing embed messages."));
		addSubcommands(createEmbedSubcommand, editEmbedSubcommand, addEmbedFieldSubcommand, removeEmbedFieldSubcommand);
	}
}