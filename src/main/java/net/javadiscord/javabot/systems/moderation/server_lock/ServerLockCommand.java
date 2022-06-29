package net.javadiscord.javabot.systems.moderation.server_lock;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.server_lock.subcommands.LockStatusSubcommand;

/**
 * Represents the `/serverlock-admin` command. This holds administrative commands for managing the server lock functionality.
 */
public class ServerLockCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public ServerLockCommand() {
		setSlashCommandData(Commands.slash("serverlock-admin", "Administrative commands for managing the server lock functionality.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new LockStatusSubcommand());
	}
}
