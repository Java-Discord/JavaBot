package net.javadiscord.javabot.systems.jam.subcommands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import io.sentry.Sentry;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * An abstract subcommand type that's used by any Jam subcommand which should
 * only operate in the context of an Active Java Jam. This parent class will
 * handle opening a connection to the data source and fetching the active jam,
 * so that clients only need to implement {@link ActiveJamSubcommand#handleJamCommand(SlashCommandInteractionEvent, Jam, Connection, JamConfig)}.
 */
public abstract class ActiveJamSubcommand extends SlashCommand.Subcommand {
	private static final Logger log = LoggerFactory.getLogger(ActiveJamSubcommand.class);

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.warning(event, "This command can only be used inside servers.");
			return;
		}
		try (Connection con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(false);
			Jam activeJam = new JamRepository(con).getActiveJam(event.getGuild().getIdLong());
			if (activeJam == null) {
				Responses.warning(event, "No Active Jam", "There is currently no active jam in this guild.").queue();
				return;
			}
			try {
				ReplyCallbackAction reply = handleJamCommand(event, activeJam, con, Bot.config.get(event.getGuild()).getJam());
				con.commit();
				reply.queue();
			} catch (SQLException e) {
				con.rollback();
				log.warn("Exception thrown while handling Jam command: {}", e.getMessage());
				Responses.error(event, "An error occurred:\n```" + e.getMessage() + "```").queue();
			}
		} catch (SQLException e) {
			Sentry.captureException(e);
			Responses.error(event, "An SQL error occurred.").queue();
		}
	}

	protected abstract ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException;
}
