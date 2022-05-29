package net.javadiscord.javabot.systems.moderation.server_lock;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.server_lock.subcommands.LockStatusSubcommand;

public class ServerLockCommand extends SlashCommand {
	public ServerLockCommand() {
		setCommandData(Commands.slash("server-lock", "Administrative commands for managing the server lock functionality.")
				// TODO: Implement App Permissions V2 once JDA releases them
				.setDefaultEnabled(false));
		setSubcommands(new LockStatusSubcommand());
	}
}
