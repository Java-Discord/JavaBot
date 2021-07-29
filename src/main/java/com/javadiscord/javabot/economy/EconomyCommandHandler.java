package com.javadiscord.javabot.economy;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.SQLException;

public class EconomyCommandHandler implements SlashCommandHandler {
	@Override
	public void handle(SlashCommandEvent event) {
		event.deferReply(true).queue();
		if (event.getSubcommandName().equals("give")) {
			try {
				new EconomyService().performTransaction(null, event.getUser().getIdLong(), 1000);
				event.getHook().sendMessage("Given!").queue();
			} catch (SQLException e) {
				e.printStackTrace();
				event.getHook().sendMessage("Error: " + e.getMessage()).queue();
			}
		} else {
			event.getHook().sendMessage("Invalid subcommand.").queue();
		}
	}
}
