package com.javadiscord.javabot.commands.jam.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.jam.JamCommandHandler;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
public abstract class ActiveJamSubcommand implements SlashCommandHandler {
	private static final Logger log = LoggerFactory.getLogger(ActiveJamSubcommand.class);
	private final boolean admin;

	@Override
	public void handle(SlashCommandEvent event) {
		if (admin && !JamCommandHandler.ensureAdmin(event)) return;
		event.deferReply(true).queue();

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
