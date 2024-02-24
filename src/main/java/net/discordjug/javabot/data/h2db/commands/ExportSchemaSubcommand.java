package net.discordjug.javabot.data.h2db.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

/**
 * <h3>This class represents the /db-admin export-schema command.</h3>
 * This Subcommand exports the database schema to a file, and uploads that file
 * to the channel in which the command was received.
 */
public class ExportSchemaSubcommand extends SlashCommand.Subcommand {
	private static final Path SCHEMA_FILE = Path.of("___schema.sql");

	private final ExecutorService asyncPool;
	private final DataSource dataSource;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param botConfig The main configuration of the bot
	 * @param dataSource A factory for connections to the main database
	 */
	public ExportSchemaSubcommand(ExecutorService asyncPool, BotConfig botConfig, DataSource dataSource) {
		this.asyncPool = asyncPool;
		this.dataSource = dataSource;
		setCommandData(new SubcommandData("export-schema", "(ADMIN ONLY) Exports the bot's schema.")
				.addOption(OptionType.BOOLEAN, "include-data", "Should data be included in the export?"));
		setRequiredUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
		setRequiredPermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		boolean includeData = event.getOption("include-data", false, OptionMapping::getAsBoolean);
		event.deferReply(false).queue();
		asyncPool.submit(() -> {
			try (Connection con = dataSource.getConnection();
					Statement stmt = con.createStatement()) {
				boolean success = stmt.execute(String.format("SCRIPT %s TO '%s';", includeData ? "" : "NODATA", SCHEMA_FILE));
				if (!success) {
					event.getHook().sendMessage("Exporting the schema was not successful.").queue();
				} else {
					event.getHook().sendMessage("The export was successful.")
							.addFiles(FileUpload.fromData(SCHEMA_FILE.toFile(), "database/schema.sql")).queue(msg -> {
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
