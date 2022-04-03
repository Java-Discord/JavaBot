package net.javadiscord.javabot.data.h2db.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.interfaces.Autocompletable;
import net.javadiscord.javabot.util.AutocompleteUtils;

import java.util.List;

/**
 * Handler class for all Database related commands.
 */
public class DbAdminCommandHandler extends DelegatingCommandHandler implements Autocompletable {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public DbAdminCommandHandler() {
		this.addSubcommand("export-schema", new ExportSchemaSubcommand());
		this.addSubcommand("export-table", new ExportTableSubcommand());
		this.addSubcommand("migrations-list", new MigrationsListSubcommand());
		this.addSubcommand("migrate", new MigrateSubcommand());
	}

	@Override
	public AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = switch (event.getSubcommandName()) {
			case "migrate" -> MigrateSubcommand.replyMigrations(event);
			default -> List.of();
		};
		return event.replyChoices(AutocompleteUtils.filterChoices(event, choices));
	}
}
