package net.discordjug.javabot.data.h2db.commands;

import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.data.h2db.MigrationUtils;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javax.sql.DataSource;

/**
 * <h3>This class represents the /db-admin migrate command.</h3>
 * This subcommand is responsible for executing SQL migrations on the bot's
 * schema.
 * <p>
 * It uses the given name (adding .sql if it's not already there) to look
 * for a matching file in the /migrations/ resource directory. Once it's
 * found the file, it will split it up into a list of statements by the ';'
 * character, and then proceed to execute each statement.
 * </p>
 */
public class MigrateSubcommand extends SlashCommand.Subcommand implements AutoCompletable {

	private final ExecutorService asyncPool;
	private final DataSource dataSource;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param dataSource A factory for connections to the main database
	 * @param systemsConfig Configuration for various systems
	 */
	public MigrateSubcommand(ExecutorService asyncPool, DataSource dataSource, SystemsConfig systemsConfig) {
		this.asyncPool = asyncPool;
		this.dataSource = dataSource;
		setCommandData(new SubcommandData("migrate", "(ADMIN ONLY) Run a single database migration")
				.addOption(OptionType.STRING, "name", "The migration's filename", true, true));
		setRequiredUsers(systemsConfig.getAdminConfig().getAdminUsers());
		setRequiredPermissions(Permission.MANAGE_SERVER);
	}

	/**
	 * Finds all all available migrations to run.
	 *
	 * @return A {@link List} with all Option Choices.
	 */
	public static @NotNull List<Command.Choice> getAvailableMigrations() {
		List<Command.Choice> choices = new ArrayList<>(25);
		try (Stream<Path> s = Files.list(MigrationUtils.getMigrationsDirectory())) {
			List<Path> paths = s.filter(path -> path.getFileName().toString().endsWith(".sql")).toList();
			paths.forEach(path -> choices.add(new Command.Choice(path.getFileName().toString(), path.getFileName().toString())));
		} catch (IOException | URISyntaxException e) {
			ExceptionLogger.capture(e, MigrateSubcommand.class.getSimpleName());
		}
		return choices;
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		String migrationName = Objects.requireNonNull(event.getOption("name")).getAsString();
		if (!migrationName.endsWith(".sql")) {
			migrationName = migrationName + ".sql";
		}
		try {
			Path migrationsDir = MigrationUtils.getMigrationsDirectory();
			Path migrationFile = migrationsDir.resolve(migrationName);
			if (Files.notExists(migrationFile)) {
				Responses.error(event, "The specified migration `" + migrationName + "` does not exist.").queue();
				return;
			}
			String sql = Files.readString(migrationFile);
			migrationsDir.getFileSystem().close();
			String[] statements = sql.split("\\s*;\\s*");
			if (statements.length == 0) {
				Responses.error(event, "The migration `" + migrationName + "` does not contain any statements. Please remove or edit it before running again.").queue();
				return;
			}
			event.deferReply().queue();
			asyncPool.submit(() -> {
				try (Connection con = dataSource.getConnection()) {
					for (int i = 0; i < statements.length; i++) {
						if (statements[i].isBlank()) {
							event.getHook().sendMessage("Skipping statement " + (i + 1) + "; it is blank.").queue();
							continue;
						}
						try (Statement stmt = con.createStatement()) {
							int rowsUpdated = stmt.executeUpdate(statements[i]);
							event.getHook().sendMessageFormat(
									"Executed statement %d of %d:\n```sql\n%s\n```\nRows Updated: `%d`", i + 1, statements.length, statements[i], rowsUpdated
							).queue();
						} catch (SQLException e) {
							ExceptionLogger.capture(e, getClass().getSimpleName());
							event.getHook().sendMessage("Error while executing statement " + (i + 1) + ": " + e.getMessage()).queue();
							return;
						}
					}
				} catch (SQLException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					event.getHook().sendMessage("Could not obtain a connection to the database.").queue();
				}
			});
			Responses.info(event.getHook(), "Migration Started",
					"Execution of the migration `" + migrationName + "` has been started. " + statements.length + " statements will be executed.").queue();
		} catch (IOException | URISyntaxException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event.getHook(), e.getMessage()).queue();
		}
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		event.replyChoices(AutoCompleteUtils.filterChoices(event, getAvailableMigrations())).queue();
	}
}
