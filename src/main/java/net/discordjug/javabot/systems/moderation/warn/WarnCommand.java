package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.systems.moderation.CommandModerationPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/warn` command. This holds administrative commands for managing user warns.
 */
public class WarnCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param warnAddSubcommand /warn add
	 * @param discardWarnByIdSubCommand /warn discard-by-id
	 * @param discardAllWarnsSubcommand /warn discard-all
	 * @param exportSubcommand /warn export
	 */
	public WarnCommand(WarnAddSubcommand warnAddSubcommand, DiscardWarnByIdSubCommand discardWarnByIdSubCommand, DiscardAllWarnsSubcommand discardAllWarnsSubcommand, WarnExportSubcommand exportSubcommand) {
		setModerationSlashCommandData(Commands.slash("warn", "Administrative commands for managing user warns."));
		addSubcommands(warnAddSubcommand, discardWarnByIdSubCommand, discardAllWarnsSubcommand, exportSubcommand);
	}
}
