package net.javadiscord.javabot.systems.qotw.subcommands.questions_queue;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.qotw.dao.QuestionQueueRepository;
import net.javadiscord.javabot.systems.qotw.subcommands.QOTWSubcommand;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Subcommand that allows staff-members to remove single questions from the QOTW Queue.
 */
public class RemoveQuestionSubcommand extends QOTWSubcommand {
	@Override
	protected ReplyAction handleCommand(SlashCommandEvent event, Connection con, long guildId) throws SQLException {
		OptionMapping idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}
		long id = idOption.getAsLong();
		boolean removed = new QuestionQueueRepository(con).removeQuestion(guildId, id);
		if (removed) {
			return Responses.success(event, "Question Removed", "The question with id `" + id + "` has been removed.");
		} else {
			return Responses.warning(event, "Could not remove question with id `" + id + "`. Are you sure it exists?");
		}
	}
}
