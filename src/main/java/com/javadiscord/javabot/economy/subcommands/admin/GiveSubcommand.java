package com.javadiscord.javabot.economy.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.economy.EconomyNotificationService;
import com.javadiscord.javabot.economy.EconomyService;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.SQLException;

public class GiveSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping userOption = event.getOption("recipient");
		OptionMapping amountOption = event.getOption("amount");
		OptionMapping messageOption = event.getOption("message");
		if (userOption == null || amountOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		if (amountOption.getAsLong() == 0) {
			return Responses.warning(event, "Cannot send a value of zero.");
		}

		long amount = amountOption.getAsLong();
		Long fromUserId = null;
		Long toUserId = null;
		String message = null;
		if (amount > 0) {
			toUserId = userOption.getAsUser().getIdLong();
		} else {
			fromUserId = userOption.getAsUser().getIdLong();
			amount *= -1;
		}
		if (messageOption != null) {
			message = messageOption.getAsString();
			if (message.length() > 127) {
				return Responses.warning(event, "Message is longer than 127 characters.");
			}
		}

		try {
			var service = new EconomyService(Bot.dataSource);
			var t = service.performTransaction(fromUserId, toUserId, amount, message);
			new EconomyNotificationService().sendTransactionNotification(t, event);
			String messageTemplate;
			if (fromUserId == null) {
				messageTemplate = "Gave `%,d` to %s.";
			} else {
				messageTemplate = "Took `%,d` from %s.";
			}
			return Responses.success(event, "Transaction Complete", String.format(messageTemplate, t.getValue(), userOption.getAsUser().getAsTag()));
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, e.getMessage());
		}
	}
}
