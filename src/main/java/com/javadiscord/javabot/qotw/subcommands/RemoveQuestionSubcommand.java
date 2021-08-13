package com.javadiscord.javabot.qotw.subcommands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.qotw.dao.QuestionRepository;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

public class RemoveQuestionSubcommand extends QOTWSubcommand {
	@Override
	protected ReplyAction handleCommand(SlashCommandEvent event, Connection con, long guildId) throws Exception {
		OptionMapping idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		long id = idOption.getAsLong();
		boolean removed = new QuestionRepository(con).removeQuestion(guildId, id);
		if (removed) {
			return Responses.success(event, "Question Removed", "The question with id `" + id + "` has been removed.");
		} else {
			return Responses.warning(event, "Could not remove question with id `" + id + "`. Are you sure it exists?");
		}
	}
}
