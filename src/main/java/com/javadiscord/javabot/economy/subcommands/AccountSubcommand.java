package com.javadiscord.javabot.economy.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.economy.EconomyService;
import com.javadiscord.javabot.economy.model.Account;
import com.javadiscord.javabot.economy.model.Transaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.RestAction;

import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AccountSubcommand implements SlashCommandHandler {
	private static final int VALUE_BUFFER_SPACE = 40;

	@Override
	public void handle(SlashCommandEvent event) {
		event.deferReply(true).queue();
		try {
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
				Executors.newSingleThreadExecutor().submit(() -> {
					try {
						this.getTransactionsString(transactions, event, account).thenAccept(s -> {
							embedBuilder.addField("Recent Transactions", "```" + s + "```", false);
							event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
						}).get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
			} else {
				event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			event.getHook().sendMessage("Error: " + e.getMessage()).queue();
		}
	}

	private CompletableFuture<String> getTransactionsString(List<Transaction> transactions, SlashCommandEvent event, Account account) {
		List<RestAction<User>> userRestActions = new ArrayList<>();
		for (var t : transactions) {
			if (t.getFromUserId() != null) {
				userRestActions.add(event.getJDA().retrieveUserById(t.getFromUserId()));
			}
			if (t.getToUserId() != null) {
				userRestActions.add(event.getJDA().retrieveUserById(t.getToUserId()));
			}
		}
		return RestAction.allOf(userRestActions).submit().thenApply(users -> {
			Map<Long, User> usersMap = users.stream().distinct().collect(Collectors.toMap(ISnowflake::getIdLong, user -> user));
			return transactions.stream()
					.map(t -> {
						String value = String.format("%,d", t.getValue());
						String otherUserString;
						if (t.getFromUserId() != null && t.getFromUserId().equals(account.getUserId())) {
							value = "- " + value;
							otherUserString = (usersMap.get(t.getToUserId()) == null ? "System" : usersMap.get(t.getToUserId()).getAsTag());
						} else {
							value = "+ " + value;
							otherUserString = (usersMap.get(t.getFromUserId()) == null ? "System" : usersMap.get(t.getFromUserId()).getAsTag());
						}
						value = " ".repeat(VALUE_BUFFER_SPACE - value.length()) + value;
						otherUserString = " ".repeat(VALUE_BUFFER_SPACE - otherUserString.length()) + otherUserString;
						return String.format("%s\n%s\n%s", t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss 'UTC'")), value, otherUserString);
					})
					.collect(Collectors.joining("\n"));
		});
	}
}
