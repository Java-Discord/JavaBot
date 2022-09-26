package net.javadiscord.javabot.data.h2db.commands;

import java.util.Map;
import java.util.Set;

import com.dynxsty.dih4jda.interactions.commands.RegistrationType;
import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.javadiscord.javabot.data.config.BotConfig;

/**
 * Represents the `/db-admin` command. This holds administrative commands for managing the bot's database.
 */
public class DbAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s & {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 * @param botConfig The main configuration of the bot
	 * @param exportSchemaSubcommand /db-admin export-schema
	 * @param exportTableSubcommand /db-admin export-table
	 * @param migrationsListSubcommand /db-admin migrations-list
	 * @param migrateSubcommand /db-admin migrate
	 * @param quickMigrateSubcommand /db-admin quick-migrate
	 * @param messageCacheInfoSubcommand /db-admin message-cache info
	 */
	public DbAdminCommand(BotConfig botConfig, ExportSchemaSubcommand exportSchemaSubcommand, ExportTableSubcommand exportTableSubcommand, MigrationsListSubcommand migrationsListSubcommand, MigrateSubcommand migrateSubcommand, QuickMigrateSubcommand quickMigrateSubcommand, MessageCacheInfoSubcommand messageCacheInfoSubcommand) {
		setRegistrationType(RegistrationType.GUILD);
		setSlashCommandData(Commands.slash("db-admin", "(ADMIN ONLY) Administrative Commands for managing the bot's database.")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
				.setGuildOnly(true)
		);
		addSubcommands(exportSchemaSubcommand, exportTableSubcommand, migrationsListSubcommand, migrateSubcommand, quickMigrateSubcommand);
		addSubcommandGroups(Map.of(
				new SubcommandGroupData("message-cache", "Administrative tools for managing the Message Cache."), Set.of(messageCacheInfoSubcommand)
		));
		requireUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}
}
