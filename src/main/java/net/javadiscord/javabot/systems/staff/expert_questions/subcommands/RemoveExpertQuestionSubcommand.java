package net.javadiscord.javabot.systems.staff.expert_questions.subcommands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.staff.expert_questions.ExpertSubcommand;
import net.javadiscord.javabot.systems.staff.expert_questions.dao.ExpertQuestionRepository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Subcommand that allows staff-members to remove expert questions.
 */
public class RemoveExpertQuestionSubcommand extends ExpertSubcommand {
	@Override
	protected ReplyAction handleCommand(SlashCommandEvent event, Connection con) throws SQLException {
		var idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		var id = idOption.getAsLong();
		var repo = new ExpertQuestionRepository(con);
		if (repo.remove(event.getGuild().getIdLong(), id)) {
			return Responses.success(event, "Removed Expert Question",
					String.format("Successfully removed Expert Question with id `%s`", id));
		} else {
			return Responses.error(event, "Could not remove Expert Question");
		}
	}
}
