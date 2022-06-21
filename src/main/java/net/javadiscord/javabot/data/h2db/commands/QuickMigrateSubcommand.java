package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <h3>This class represents the /db-admin quick-migrate command.</h3>
 * This subcommand is responsible for executing quick SQL migrations on the bot's
 * schema.
 */
public class QuickMigrateSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public QuickMigrateSubcommand() {
		setSubcommandData(new SubcommandData("quick-migrate", "(ADMIN ONLY) Run a single quick database migration")
				.addOption(OptionType.STRING, "sql-statement", "The SQL-Statement to run", true, true)
				.addOption(OptionType.STRING, "confirmation", "Type \"CONFIRM\" to confirm this action.")
		);
		requireUsers(Bot.config.getSystems().getAdminUsers());
		requirePermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping statementMapping = event.getOption("sql-statement");
		OptionMapping confirmMapping = event.getOption("confirmation");
		if (statementMapping == null || confirmMapping == null) {
			Responses.error(event, "Missing required arguments").queue();
			return;
		}
		if (!confirmMapping.getAsString().equals("CONFIRM")) {
			Responses.error(event, "Invalid confirmation. Please try again.").queue();
			return;
		}
		String sql = statementMapping.getAsString();
		String[] statements = sql.split("\\s*;\\s*");
		if (statements.length == 0) {
			Responses.error(event, "The provided migration does not contain any statements. Please remove or edit it before running again.").queue();
			return;
		}
		Bot.asyncPool.submit(() -> {
			try (Connection con = Bot.dataSource.getConnection()) {
				for (int i = 0; i < statements.length; i++) {
					if (statements[i].isBlank()) {
						event.getChannel().sendMessage("Skipping statement " + (i + 1) + "; it is blank.").queue();
						continue;
					}
					try (Statement stmt = con.createStatement()) {
						int rowsUpdated = stmt.executeUpdate(statements[i]);
						event.getChannel().sendMessageFormat(
								"Executed statement %d of %d:\n```sql\n%s\n```\nRows Updated: `%d`", i + 1, statements.length, statements[i], rowsUpdated
						).queue();
					} catch (SQLException e) {
						ExceptionLogger.capture(e, getClass().getSimpleName());
						event.getChannel().sendMessage("Error while executing statement " + (i + 1) + ": " + e.getMessage()).queue();
						return;
					}
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				event.getChannel().sendMessage("Could not obtain a connection to the database.").queue();
			}
		});
		Responses.info(event, "Quick Migration Started",
				"Execution of the quick migration has been started. " + statements.length + " statements will be executed.").queue();
	}
}
