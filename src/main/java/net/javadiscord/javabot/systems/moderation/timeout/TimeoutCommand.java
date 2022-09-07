package net.javadiscord.javabot.systems.moderation.timeout;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;
import net.javadiscord.javabot.systems.notification.NotificationService;

/**
 * Handler class for all timeout specific commands.
 */
public class TimeoutCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 */
	public TimeoutCommand(NotificationService notificationService, BotConfig botConfig, DbHelper dbHelper) {
		setModerationSlashCommandData(Commands.slash("timeout", "Commands for managing member timeouts."));
		addSubcommands(new AddTimeoutSubcommand(notificationService, botConfig, dbHelper), new RemoveTimeoutSubcommand(notificationService, botConfig, dbHelper));
	}
}
