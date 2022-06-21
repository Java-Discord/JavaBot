package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import io.sentry.Sentry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * This subcommand exports the database schema to a file, and uploads that file
 * to the channel in which the command was received.
 */
public class ExportSchemaSubcommand extends SlashCommand.Subcommand {
	private static final Path SCHEMA_FILE = Path.of("___schema.sql");

	public ExportSchemaSubcommand() {
		setSubcommandData(new SubcommandData("export-schema", "(ADMIN ONLY) Exports the bot's schema.")
				.addOption(OptionType.BOOLEAN, "include-data", "Should data be included in the export?"));
		requireUsers(Bot.config.getSystems().getAdminUsers());
		requirePermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		boolean includeData = event.getOption("include-data", false, OptionMapping::getAsBoolean);
		event.deferReply(false).queue();
		Bot.asyncPool.submit(() -> {
			try (var con = Bot.dataSource.getConnection()) {
				var stmt = con.createStatement();
				boolean success = stmt.execute(String.format("SCRIPT %s TO '%s';", includeData ? "" : "NODATA", SCHEMA_FILE));
				if (!success) {
					event.getHook().sendMessage("Exporting the schema was not successful.").queue();
				} else {
					event.getHook().sendMessage("The export was successful.").addFile(SCHEMA_FILE.toFile(), "schema.sql").queue(msg -> {
						try {
							Files.delete(SCHEMA_FILE);
						} catch (IOException e) {
							ExceptionLogger.capture(e, getClass().getSimpleName());
							event.getHook().sendMessage("An error occurred, and the export could not be made: " + e.getMessage()).queue();
						}
					});
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				event.getHook().sendMessage("An error occurred, and the export could not be made: " + e.getMessage()).queue();
			}
		});
	}
}
