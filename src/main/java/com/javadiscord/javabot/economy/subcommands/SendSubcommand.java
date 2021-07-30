package com.javadiscord.javabot.economy.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.economy.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.SQLException;

public class SendSubcommand implements SlashCommandHandler {
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

		try {
			var service = new EconomyService(Bot.dataSource);
			var account = service.getOrCreateAccount(event.getUser().getIdLong());
			if (account.getBalance() < amountOption.getAsLong()) {
				event.getHook().sendMessage("Your balance is not sufficient to send the funds.").queue();
				return;
			}
			var t = service.performTransaction(event.getUser().getIdLong(), userOption.getAsUser().getIdLong(), amountOption.getAsLong());
			account = service.getOrCreateAccount(event.getUser().getIdLong());
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Transaction Successful")
					.setDescription(String.format("Successfully sent `%,d` to %s.", t.getValue(), userOption.getAsUser().getAsTag()))
					.addField("Account Status", String.format("Your account's balance is now `%,d`. Use `/economy account` for more information.", account.getBalance()), false)
					.setTimestamp(t.getCreatedAt());
			event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
		} catch (SQLException e) {
			e.printStackTrace();
			event.getHook().sendMessage("Error: " + e.getMessage()).queue();
		}
	}
}
