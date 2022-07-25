package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <h3>This class represents the /db-admin export-table command.</h3>
 * This subcommand exports a single database table to a file, and uploads that file
 * to the channel in which the command was received.
 */
public class ExportTableSubcommand extends SlashCommand.Subcommand {
	private static final Path TABLE_FILE = Path.of("___table.sql");

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public ExportTableSubcommand() {
		setSubcommandData(new SubcommandData("export-table", "(ADMIN ONLY) Export a single database table")
				.addOptions(new OptionData(OptionType.STRING, "table", "What table should be exported", true)
								.addChoice("Custom Tags", "CUSTOM_TAGS")
								.addChoice("Help Account", "HELP_ACCOUNT")
								.addChoice("Help Channel Thanks", "HELP_CHANNEL_THANKS")
								.addChoice("Help Transactions", "HELP_TRANSACTION")
								.addChoice("Message Cache", "MESSAGE_CACHE")
								.addChoice("Question of the Week Accounts", "QOTW_POINTS")
								.addChoice("Question of the Week Questions", "QOTW_QUESTION")
								.addChoice("Question of the Week Submissions", "QOTW_SUBMISSIONS")
								.addChoice("Reserved Help Channels", "RESERVED_HELP_CHANNELS")
								.addChoice("Starboard", "STARBOARD")
								.addChoice("Warns", "WARN"),
						new OptionData(OptionType.BOOLEAN, "include-data", "Should data be included in the export?")));
		requireUsers(Bot.config.getSystems().getAdminConfig().getAdminUsers());
		requirePermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping tableOption = event.getOption("table");
		boolean includeData = event.getOption("include-data", false, OptionMapping::getAsBoolean);
		if (tableOption == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		event.deferReply(false).queue();
		Bot.asyncPool.submit(() -> {
			try (Connection con = Bot.dataSource.getConnection()) {
				PreparedStatement stmt = con.prepareStatement(String.format("SCRIPT %s TO '%s' TABLE %s;", includeData ? "COLUMNS" : "NODATA", TABLE_FILE, tableOption.getAsString()));
				boolean success = stmt.execute();
				if (!success) {
					event.getHook().sendMessage("Exporting the table was not successful.").queue();
				} else {
					event.getHook().sendMessage("The export was successful.").addFile(TABLE_FILE.toFile(), "table.sql").queue(msg -> {
						try {
							Files.delete(TABLE_FILE);
						} catch (IOException e) {
							ExceptionLogger.capture(e, getClass().getSimpleName());
							event.getHook().sendMessageFormat("An error occurred, and the export could not be made: ```\n%s\n```", e.getMessage()).queue();
						}
					});
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				event.getHook().sendMessageFormat("An error occurred, and the export could not be made: ```\n%s\n```", e.getMessage()).queue();
			}
		});
	}
}
