package com.javadiscord.javabot.economy.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.economy.EconomyService;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.SQLException;

public class GiveSubcommand implements SlashCommandHandler {
	@Override
	public void handle(SlashCommandEvent event) {
		event.deferReply(true).queue();
		OptionMapping userOption = event.getOption("recipient");
		OptionMapping amountOption = event.getOption("amount");
		if (userOption == null || amountOption == null) {
			event.getHook().sendMessage("Missing required arguments.").queue();
			return;
		}

		if (amountOption.getAsLong() == 0) {
			event.getHook().sendMessage("Cannot send a value of zero.").queue();
			return;
		}

		long amount = amountOption.getAsLong();
		Long fromUserId = null;
		Long toUserId = null;
		if (amount > 0) {
			toUserId = userOption.getAsUser().getIdLong();
		} else {
			fromUserId = userOption.getAsUser().getIdLong();
			amount *= -1;
		}

		try {
			var service = new EconomyService(Bot.dataSource);
			var t = service.performTransaction(fromUserId, toUserId, amount);
			String messageTemplate;
			if (fromUserId == null) {
				messageTemplate = "Gave `%,d` to %s.";
			} else {
				messageTemplate = "Took `%,d` from %s.";
			}
			event.getHook().sendMessage(String.format(messageTemplate, t.getValue(), userOption.getAsUser().getAsTag())).queue();
		} catch (SQLException e) {
			e.printStackTrace();
			event.getHook().sendMessage("Error: " + e.getMessage()).queue();
		}
	}
}
