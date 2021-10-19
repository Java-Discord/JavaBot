package com.javadiscord.javabot.service.jam.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.service.jam.dao.JamRepository;
import com.javadiscord.javabot.service.jam.model.Jam;
import com.javadiscord.javabot.data.properties.config.guild.JamConfig;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * An abstract subcommand type that's used by any Jam subcommand which should
 * only operate in the context of an Active Java Jam. This parent class will
 * handle opening a connection to the data source and fetching the active jam,
 * so that clients only need to implement {@link ActiveJamSubcommand#handleJamCommand(SlashCommandEvent, Jam, Connection, JamConfig)}.
 */
public abstract class ActiveJamSubcommand implements SlashCommandHandler {
	private static final Logger log = LoggerFactory.getLogger(ActiveJamSubcommand.class);

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
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
			} catch (Throwable e) {
				con.rollback();
				log.warn("Exception thrown while handling Jam command: {}", e.getMessage());
				return Responses.error(event, "An error occurred:\n```" + e.getMessage() + "```");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An SQL error occurred.");
		}
	}

	protected abstract ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws Exception;
}
