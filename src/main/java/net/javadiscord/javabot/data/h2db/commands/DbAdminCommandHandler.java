package net.javadiscord.javabot.data.h2db.commands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.data.h2db.commands.message_cache.MessageCacheInfoSubcommand;

import java.util.Map;

/**
 * Handler class for all Database related commands.
 */
public class DbAdminCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public DbAdminCommandHandler() {
		this.addSubcommand("export-schema", new ExportSchemaSubcommand());
		this.addSubcommand("export-table", new ExportTableSubcommand());
		this.addSubcommand("migrations-list", new MigrationsListSubcommand());
		this.addSubcommand("migrate", new MigrateSubcommand());

		this.addSubcommandGroup("message-cache", new DelegatingCommandHandler(Map.of(
				"info", new MessageCacheInfoSubcommand()
		)));
	}
}
