package net.javadiscord.javabot.data.h2db.commands;

import net.javadiscord.javabot.command.DelegatingCommandHandler;

public class DbAdminCommandHandler extends DelegatingCommandHandler {
	public DbAdminCommandHandler() {
		this.addSubcommand("export-schema", new ExportSchemaSubcommand());
		this.addSubcommand("migrations-list", new MigrationsListSubcommand());
		this.addSubcommand("migrate", new MigrateSubcommand());
	}
}
