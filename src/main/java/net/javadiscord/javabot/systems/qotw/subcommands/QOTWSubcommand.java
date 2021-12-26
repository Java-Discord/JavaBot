package net.javadiscord.javabot.systems.qotw.subcommands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.sql.Connection;

/**
 * Abstract parent class for all QOTW subcommands, which handles the standard
 * behavior of preparing a connection and obtaining the guild id; these two
 * things are required for all QOTW subcommands.
 */
public abstract class QOTWSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		if (event.getGuild() == null) {
			return Responses.warning(event, "This command can only be used in the context of a guild.");
		}

		try (Connection con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(false);
			var reply = this.handleCommand(event, con, event.getGuild().getIdLong());
			con.commit();
			return reply;
		} catch (Exception e) {
			e.printStackTrace();
			return Responses.error(event, "An error occurred: " + e.getMessage());
		}
	}

	protected abstract ReplyAction handleCommand(SlashCommandEvent event, Connection con, long guildId) throws Exception;
}
