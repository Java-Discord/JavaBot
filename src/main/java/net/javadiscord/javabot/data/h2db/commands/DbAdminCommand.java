package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.RegistrationType;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.data.h2db.message_cache.MessageCache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

/**
 * Represents the `/db-admin` command. This holds administrative commands for managing the bot's database.
 */
public class DbAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s & {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param messageCache A service managing recent messages
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param botConfig The main configuration of the bot
	 * @param dataSource A factory for connections to the main database
	 * @param dbActions A utility object providing various operations on the main database
	 */
	public DbAdminCommand(MessageCache messageCache, ExecutorService asyncPool, BotConfig botConfig, DataSource dataSource, DbActions dbActions) {
		setRegistrationType(RegistrationType.GUILD);
		setSlashCommandData(Commands.slash("db-admin", "(ADMIN ONLY) Administrative Commands for managing the bot's database.")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
				.setGuildOnly(true)
		);
		addSubcommands(new ExportSchemaSubcommand(asyncPool, botConfig, dataSource), new ExportTableSubcommand(asyncPool, botConfig.getSystems(), dataSource), new MigrationsListSubcommand(botConfig.getSystems()), new MigrateSubcommand(asyncPool, dataSource, botConfig.getSystems()), new QuickMigrateSubcommand(dataSource, asyncPool, botConfig.getSystems()));
		addSubcommandGroups(Map.of(
				new SubcommandGroupData("message-cache", "Administrative tools for managing the Message Cache."), Set.of(new MessageCacheInfoSubcommand(messageCache, botConfig, dbActions))
		));
		requireUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}
}
