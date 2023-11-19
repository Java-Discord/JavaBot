package net.discordjug.javabot.data.h2db.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.RegistrationType;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

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
		setCommandData(Commands.slash("db-admin", "(ADMIN ONLY) Administrative Commands for managing the bot's database.")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
				.setGuildOnly(true)
		);
		addSubcommands(exportSchemaSubcommand, exportTableSubcommand, migrationsListSubcommand, migrateSubcommand, quickMigrateSubcommand);
		addSubcommandGroups(SubcommandGroup.of(
				new SubcommandGroupData("message-cache", "Administrative tools for managing the Message Cache."), messageCacheInfoSubcommand
		));
		setRequiredUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}
}
