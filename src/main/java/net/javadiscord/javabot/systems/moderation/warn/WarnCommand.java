package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;

/**
 * Represents the `/warn` command. This holds administrative commands for managing user warns.
 */
public class WarnCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public WarnCommand() {
		setModerationSlashCommandData(Commands.slash("warn", "Administrative commands for managing user warns."));
		addSubcommands(new WarnAddSubcommand(), new DiscardWarnByIdSubCommand(), new DiscardAllWarnsSubcommand());
	}
}
