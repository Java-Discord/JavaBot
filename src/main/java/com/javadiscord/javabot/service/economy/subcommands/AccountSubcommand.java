package com.javadiscord.javabot.service.economy.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.service.economy.EconomyService;
import com.javadiscord.javabot.service.economy.model.Account;
import com.javadiscord.javabot.service.economy.model.Transaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * This subcommand shows a user's account information.
 */
public class AccountSubcommand implements SlashCommandHandler {
	private static final int VALUE_BUFFER_SPACE = 40;

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		Bot.asyncPool.submit(() -> {
			try {
				sendAccountEmbed(event);
			} catch (SQLException e) {
				e.printStackTrace();
				event.getHook().sendMessage("An SQL error occurred: " + e.getMessage()).queue();
			}
		});
		return event.deferReply(true);
	}

	private void sendAccountEmbed(SlashCommandEvent event) throws SQLException {
		var service = new EconomyService(Bot.dataSource);
		Account account = service.getOrCreateAccount(event.getUser().getIdLong());
		var transactions = service.getRecentTransactions(account.getUserId(), 5);

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Account Information for " + event.getUser().getAsTag())
				.setTimestamp(Instant.now())
				.setThumbnail(event.getUser().getAvatarUrl())
				.addField("Balance", String.format("`%,d`", account.getBalance()), false)
				.setFooter("User ID: " + account.getUserId());
		if (!transactions.isEmpty()) {
			embedBuilder.addField("Recent Transactions", this.getTransactionsString(transactions, account, event), false);
		}
		event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
	}

	private String getTransactionsString(List<Transaction> transactions, Account account, SlashCommandEvent event) {
		List<String> transactionStrings = new ArrayList<>(transactions.size() * 2);
		for (var t : transactions) {
			String value = String.format("%,d", t.getValue());
			String otherUserString = "System";
			// Check if the transaction involves us sending money to someone.
			if (t.getFromUserId() != null && t.getFromUserId().equals(account.getUserId())) {
				value = "- " + value;
				if (t.getToUserId() != null) {
					otherUserString = event.getJDA().retrieveUserById(t.getToUserId()).complete().getAsTag();
				}
			} else { // Otherwise, someone is sending money to us.
				value = "+ " + value;
				if (t.getFromUserId() != null) {
					otherUserString = event.getJDA().retrieveUserById(t.getFromUserId()).complete().getAsTag();
				}
			}
			value = " ".repeat(VALUE_BUFFER_SPACE - value.length()) + value;
			otherUserString = " ".repeat(VALUE_BUFFER_SPACE - otherUserString.length()) + otherUserString;
			transactionStrings.add(String.format(
					"%s\n%s\n%s",
					t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss 'UTC'")),
					value,
					otherUserString
			));
			if (t.getMessage() != null) {
				transactionStrings.add("Msg: \"" + t.getMessage() + "\"");
			}
		}
		return "```" + String.join("\n", transactionStrings) + "```";
	}
}
