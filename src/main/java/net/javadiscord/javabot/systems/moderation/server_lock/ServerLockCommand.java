package net.javadiscord.javabot.systems.moderation.server_lock;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.server_lock.subcommands.LockStatusSubcommand;

public class ServerLockCommand extends SlashCommand {
	public ServerLockCommand() {
		setSlashCommandData(Commands.slash("server-lock", "Administrative commands for managing the server lock functionality.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new LockStatusSubcommand());
	}
}
