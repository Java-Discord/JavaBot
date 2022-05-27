package net.javadiscord.javabot.data.h2db.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.MigrationUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This subcommand is responsible for executing SQL migrations on the bot's
 * schema.
 * <p>
 * It uses the given name (adding .sql if it's not already there) to look
 * for a matching file in the /migrations/ resource directory. Once it's
 * found the file, it will split it up into a list of statements by the ';'
 * character, and then proceed to execute each statement.
 * </p>
 */
public class MigrateSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String migrationName = Objects.requireNonNull(event.getOption("name")).getAsString();
		if (!migrationName.endsWith(".sql")) {
			migrationName = migrationName + ".sql";
		}
		try {
			Path migrationsDir = MigrationUtils.getMigrationsDirectory();
			Path migrationFile = migrationsDir.resolve(migrationName);
			if (Files.notExists(migrationFile)) {
				return Responses.warning(event, "The specified migration `" + migrationName + "` does not exist.");
			}
			String sql = Files.readString(migrationFile);
			String[] statements = sql.split("\\s*;\\s*");
			if (statements.length == 0) {
				return Responses.warning(event, "The migration `" + migrationName + "` does not contain any statements. Please remove or edit it before running again.");
			}
			Bot.asyncPool.submit(() -> {
				try (var con = Bot.dataSource.getConnection()) {
					for (int i = 0; i < statements.length; i++) {
						if (statements[i].isBlank()) {
							event.getChannel().sendMessage("Skipping statement " + (i + 1) + "; it is blank.").queue();
							continue;
						}
						try (var stmt = con.createStatement()) {
							int rowsUpdated = stmt.executeUpdate(statements[i]);
							event.getChannel().sendMessageFormat(
									"Executed statement %d of %d:\n```sql\n%s\n```\nRows Updated: `%d`", i + 1, statements.length, statements[i], rowsUpdated
							).queue();
						} catch (SQLException e) {
							e.printStackTrace();
							event.getChannel().sendMessage("Error while executing statement " + (i + 1) + ": " + e.getMessage()).queue();
							return;
						}
					}
				} catch (SQLException e) {
					event.getChannel().sendMessage("Could not obtain a connection to the database.").queue();
				}
			});
			return Responses.info(event, "Migration Started", "Execution of the migration `" + migrationName + "` has been started. " + statements.length + " statements will be executed.");
		} catch (IOException | URISyntaxException e) {
			return Responses.error(event, e.getMessage());
		}
	}

	/**
	 * Replies with all available migrations to run.
	 *
	 * @param event The {@link CommandAutoCompleteInteractionEvent} that was fired.
	 * @return A {@link List} with all Option Choices.
	 */
	public static List<Command.Choice> replyMigrations(CommandAutoCompleteInteractionEvent event) {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (var s = Files.list(MigrationUtils.getMigrationsDirectory())) {
			var paths = s.filter(path -> path.getFileName().toString().endsWith(".sql")).toList();
			paths.forEach(path -> choices.add(new Command.Choice(path.getFileName().toString(), path.getFileName().toString())));
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return choices;
	}
}
