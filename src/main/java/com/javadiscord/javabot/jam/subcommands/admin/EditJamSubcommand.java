package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;

public class EditJamSubcommand extends ActiveJamSubcommand {
	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		OptionMapping propertyNameOption = event.getOption("property");
		OptionMapping propertyValueOption = event.getOption("value");

		// TODO: Implement editing of end date.

		return Responses.warning(event, "This command is still work-in-progress.");
	}
}
