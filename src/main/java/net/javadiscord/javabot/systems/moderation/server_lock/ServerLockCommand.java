package net.javadiscord.javabot.systems.moderation.server_lock;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;

/**
 * Represents the `/serverlock-admin` command. This holds administrative commands for managing the server lock functionality.
 */
public class ServerLockCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param serverLockManager the service containing functionality regarding the server lock
	 * @param botConfig The main configuration of the bot
	 */
	public ServerLockCommand(ServerLockManager serverLockManager, BotConfig botConfig) {
		setModerationSlashCommandData(Commands.slash("serverlock-admin", "Administrative commands for managing the server lock functionality."));
		addSubcommands(new SetLockStatusSubcommand(serverLockManager, botConfig), new CheckLockStatusSubcommand(botConfig));
	}
}
