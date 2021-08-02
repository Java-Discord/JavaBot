package com.javadiscord.javabot.economy.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.economy.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.SQLException;

public class SendSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		event.deferReply(true).queue();
		OptionMapping userOption = event.getOption("recipient");
		OptionMapping amountOption = event.getOption("amount");
		if (userOption == null || amountOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		long amount = amountOption.getAsLong();
		User fromUser = event.getUser();
		User toUser = userOption.getAsUser();

		if (toUser.isBot() || toUser.isSystem() || toUser.equals(fromUser)) {
			return Responses.warning(event, "Cannot send funds to this user.");
		}

		if (amountOption.getAsLong() <= 0) {
			return Responses.warning(event, "Cannot send a non-positive amount.");
		}

		try {
			var service = new EconomyService(Bot.dataSource);
			var account = service.getOrCreateAccount(event.getUser().getIdLong());
			if (account.getBalance() < amount) {
				return Responses.warning(event, String.format("Your balance of `%,d` is not sufficient to send the funds.", account.getBalance()));
			}
			var t = service.performTransaction(fromUser.getIdLong(), toUser.getIdLong(), amount);
			account = service.getOrCreateAccount(fromUser.getIdLong());
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Transaction Successful")
					.setDescription(String.format("Successfully sent `%,d` to %s.", t.getValue(), toUser.getAsTag()))
					.addField("Account Status", String.format("Your account's balance is now `%,d`. Use `/economy account` for more information.", account.getBalance()), false)
					.setTimestamp(t.getCreatedAt());
			return event.replyEmbeds(embedBuilder.build());
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, e.getMessage());
		}
	}
}
