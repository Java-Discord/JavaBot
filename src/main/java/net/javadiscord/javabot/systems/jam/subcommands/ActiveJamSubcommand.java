package net.javadiscord.javabot.systems.jam.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
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
public abstract class ActiveJamSubcommand implements SlashCommand {
	private static final Logger log = LoggerFactory.getLogger(ActiveJamSubcommand.class);

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			return Responses.warning(event, "This command can only be used in a guild.");
		}

		try (Connection con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(false);
			Jam activeJam = new JamRepository(con).getActiveJam(event.getGuild().getIdLong());
			if (activeJam == null) {
				return Responses.warning(event, "No Active Jam", "There is currently no active jam in this guild.");
			}
			try {
				var reply = this.handleJamCommand(event, activeJam, con, Bot.config.get(event.getGuild()).getJam());
				con.commit();
				return reply;
			} catch (SQLException e) {
				con.rollback();
				log.warn("Exception thrown while handling Jam command: {}", e.getMessage());
				return Responses.error(event, "An error occurred:\n```" + e.getMessage() + "```");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An SQL error occurred.");
		}
	}

	protected abstract ReplyCallbackAction handleJamCommand(SlashCommandInteractionEvent event, Jam activeJam, Connection con, JamConfig config) throws SQLException;
}
