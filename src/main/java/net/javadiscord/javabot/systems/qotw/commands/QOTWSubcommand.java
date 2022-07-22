package net.javadiscord.javabot.systems.qotw.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract parent class for all QOTW subcommands, which handles the standard
 * behavior of preparing a connection and obtaining the guild id; these two
 * things are required for all QOTW subcommands.
 */
public abstract class QOTWSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.guildOnly(event).queue();
			return;
		}
		try (Connection con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(false);
			InteractionCallbackAction<?> reply = handleCommand(event, con, event.getGuild().getIdLong());
			con.commit();
			reply.queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, "An error occurred: " + e.getMessage()).queue();
		}
	}

	protected abstract InteractionCallbackAction<?> handleCommand(SlashCommandInteractionEvent event, Connection con, long guildId) throws SQLException;
}
