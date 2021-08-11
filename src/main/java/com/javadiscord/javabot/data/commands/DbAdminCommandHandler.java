package com.javadiscord.javabot.data.commands;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;

public class DbAdminCommandHandler extends DelegatingCommandHandler {
	public DbAdminCommandHandler() {
		this.addSubcommand("export-schema", new ExportSchemaSubcommand());
	}
}
