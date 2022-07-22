package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * <h3>This class represents the /db-admin quick-migrate command.</h3>
 * This subcommand is responsible for executing quick SQL migrations on the bot's
 * schema.
 */
public class QuickMigrateSubcommand extends SlashCommand.Subcommand implements ModalHandler {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public QuickMigrateSubcommand() {
		setSubcommandData(new SubcommandData("quick-migrate", "(ADMIN ONLY) Run a single quick database migration"));
		requireUsers(Bot.config.getSystems().getAdminUsers());
		requirePermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyModal(buildQuickMigrateModal()).queue();
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, List<ModalMapping> values) {
		ModalMapping statementMapping = event.getValue("sql");
		ModalMapping confirmMapping = event.getValue("confirmation");
		event.deferReply(false).queue();
		if (statementMapping == null || confirmMapping == null) {
			Responses.replyMissingArguments(event.getHook()).queue();
			return;
		}
		if (!confirmMapping.getAsString().equals("CONFIRM")) {
			Responses.error(event.getHook(), "Invalid confirmation. Please try again.").queue();
			return;
		}
		String sql = statementMapping.getAsString();
		String[] statements = sql.split("\\s*;\\s*");
		if (statements.length == 0) {
			Responses.error(event.getHook(), "The provided migration does not contain any statements. Please remove or edit it before running again.").queue();
			return;
		}
		Bot.asyncPool.submit(() -> {
			TextChannel channel = event.getChannel().asTextChannel();
			try (Connection con = Bot.dataSource.getConnection()) {
				for (int i = 0; i < statements.length; i++) {
					if (statements[i].isBlank()) {
						channel.sendMessage("Skipping statement " + (i + 1) + "; it is blank.").queue();
						continue;
					}
					try (Statement stmt = con.createStatement()) {
						int rowsUpdated = stmt.executeUpdate(statements[i]);
						channel.sendMessageFormat(
								"Executed statement %d of %d:\n```sql\n%s\n```\nRows Updated: `%d`", i + 1, statements.length, statements[i], rowsUpdated
						).queue();
					} catch (SQLException e) {
						ExceptionLogger.capture(e, getClass().getSimpleName());
						channel.sendMessage("Error while executing statement " + (i + 1) + ": " + e.getMessage()).queue();
						return;
					}
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				channel.sendMessage("Could not obtain a connection to the database.").queue();
			}
		});
		Responses.info(event.getHook(), "Quick Migration Started",
				"Execution of the quick migration has been started. " + statements.length + " statements will be executed.").queue();
	}

	private @NotNull Modal buildQuickMigrateModal() {
		TextInput sqlInput = TextInput.create("sql", "SQL-Statement (H2)", TextInputStyle.PARAGRAPH)
				.setPlaceholder("""
						CREATE TABLE my_table (
							thread_id BIGINT PRIMARY KEY,
							[...]
						);
						""")
				.setRequired(true)
				.build();
		TextInput confirmInput = TextInput.create("confirmation", "Confirmation", TextInputStyle.SHORT)
				.setPlaceholder("Type 'CONFIRM' to confirm this action")
				.setRequired(true)
				.build();
		return Modal.create("quick-migrate", "Quick Migrate")
				.addActionRows(ActionRow.of(sqlInput), ActionRow.of(confirmInput))
				.build();
	}
}
