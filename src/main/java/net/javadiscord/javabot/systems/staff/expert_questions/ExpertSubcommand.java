package net.javadiscord.javabot.systems.staff.expert_questions;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface class that handles all Expert Question Commands.
 */
public abstract class ExpertSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			return Responses.warning(event, "This command can only be used in the context of a guild.");
		}
		try (Connection con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(false);
			var reply = this.handleCommand(event, con);
			con.commit();
			return reply;
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An error occurred: " + e.getMessage());
		}
	}

	protected abstract ReplyCallbackAction handleCommand(SlashCommandInteractionEvent event, Connection con) throws SQLException;
}