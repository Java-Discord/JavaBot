package net.javadiscord.javabot.data.h2db.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * This subcommand exports a single database table to a file, and uploads that file
 * to the channel in which the command was received.
 */
public class ExportTableSubcommand implements SlashCommand {
	private static final Path TABLE_FILE = Path.of("___table.sql");

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var tableNameOption = event.getOption("table");
		boolean includeData = event.getOption("include-data", false, OptionMapping::getAsBoolean);
		if (tableNameOption == null) return Responses.error(event, "Missing required Choice Option");
		Bot.asyncPool.submit(() -> {
			try (var con = Bot.dataSource.getConnection()) {
				var stmt = con.createStatement();
				boolean success = stmt.execute(String.format("SCRIPT %s TO '%s' TABLE %s;", includeData ? "COLUMNS" : "NODATA", TABLE_FILE, tableNameOption.getAsString()));
				if (!success) {
					event.getHook().sendMessage("Exporting the table was not successful.").queue();
				} else {
					event.getHook().sendMessage("The export was successful.").queue();
					event.getChannel().sendFile(TABLE_FILE.toFile(), "table.sql").queue(msg -> {
						try {
							Files.delete(TABLE_FILE);
						} catch (IOException e) {
							e.printStackTrace();
							event.getHook().sendMessageFormat("An error occurred, and the export could not be made: ```\n%s\n```", e.getMessage()).queue();
						}
					});
				}
			} catch (SQLException e) {
				e.printStackTrace();
				event.getHook().sendMessageFormat("An error occurred, and the export could not be made: ```\n%s\n```", e.getMessage()).queue();
			}
		});
		return event.deferReply(true);
	}
}
