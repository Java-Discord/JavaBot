package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;
import net.javadiscord.javabot.systems.notification.NotificationService;

/**
 * Represents the `/warn` command. This holds administrative commands for managing user warns.
 */
public class WarnCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 */
	public WarnCommand(NotificationService notificationService, BotConfig botConfig, DbHelper dbHelper) {
		setModerationSlashCommandData(Commands.slash("warn", "Administrative commands for managing user warns."));
		addSubcommands(new WarnAddSubcommand(notificationService, botConfig, dbHelper), new DiscardWarnByIdSubCommand(notificationService, botConfig, dbHelper), new DiscardAllWarnsSubcommand(notificationService, botConfig, dbHelper));
	}
}
