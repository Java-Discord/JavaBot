package com.javadiscord.javabot.data.h2db.commands;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;

public class DbAdminCommandHandler extends DelegatingCommandHandler {
	public DbAdminCommandHandler() {
		this.addSubcommand("export-schema", new ExportSchemaSubcommand());
		this.addSubcommand("migrations-list", new MigrationsListSubcommand());
		this.addSubcommand("migrate", new MigrateSubcommand());
	}
}
