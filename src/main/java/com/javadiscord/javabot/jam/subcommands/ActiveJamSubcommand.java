package com.javadiscord.javabot.jam.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.jam.dao.JamRepository;
import com.javadiscord.javabot.jam.model.Jam;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * An abstract subcommand type that's used by any Jam subcommand which should
 * only operate in the context of an Active Java Jam. This parent class will
 * handle opening a connection to the data source and fetching the active jam,
 * so that clients only need to implement {@link ActiveJamSubcommand#handleJamCommand(SlashCommandEvent, Jam, Connection)}.
 */
public abstract class ActiveJamSubcommand implements SlashCommandHandler {
	private static final Logger log = LoggerFactory.getLogger(ActiveJamSubcommand.class);

	@Override
	public void handle(SlashCommandEvent event) {
		event.deferReply(true).queue();
		if (event.getGuild() == null) {
			event.getHook().sendMessage("This command only be used in a guild.").queue();
			return;
		}

		try (Connection con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(false);
			Jam activeJam = new JamRepository(con).getActiveJam(event.getGuild().getIdLong());
			if (activeJam == null) {
				event.getHook().sendMessage("No active Jam in this guild.").queue();
				return;
			}
			try {
				this.handleJamCommand(event, activeJam, con);
				con.commit();
			} catch (Throwable e) {
				con.rollback();
				event.getHook().sendMessage("An error occurred: " + e.getMessage()).queue();
				log.warn("Exception thrown while handling Jam command: " + e.getMessage());
			}
		} catch (SQLException e) {
			event.getHook().sendMessage("An SQL error occurred: " + e.getMessage()).queue();
			e.printStackTrace();
		}
	}

	protected abstract void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception;
}
