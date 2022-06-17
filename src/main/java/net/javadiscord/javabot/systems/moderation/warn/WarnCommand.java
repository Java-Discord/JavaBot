package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardAllWarnsSubcommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.DiscardWarnByIdSubCommand;
import net.javadiscord.javabot.systems.moderation.warn.subcommands.WarnAddSubcommand;

/**
 * Represents the `/warn` command. This holds administrative commands for managing user warns.
 */
public class WarnCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public WarnCommand() {
		setSlashCommandData(Commands.slash("warn", "Administrative commands for managing user warns.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new WarnAddSubcommand(), new DiscardWarnByIdSubCommand(), new DiscardAllWarnsSubcommand());
	}
}
