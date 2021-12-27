package net.javadiscord.javabot.systems.economy.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.economy.EconomyNotificationService;
import net.javadiscord.javabot.systems.economy.EconomyService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SendSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping userOption = event.getOption("recipient");
		OptionMapping amountOption = event.getOption("amount");
		OptionMapping messageOption = event.getOption("message");
		if (userOption == null || amountOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		long amount = amountOption.getAsLong();
		User fromUser = event.getUser();
		User toUser = userOption.getAsUser();
		String message = null;
		if (messageOption != null) {
			message = messageOption.getAsString();
			var errors = validateMessage(message);
			if (!errors.isEmpty()) {
				return Responses.warning(event, "Your message is invalid:\n" + String.join("\n", errors));
			}
		}

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
			var t = service.performTransaction(fromUser.getIdLong(), toUser.getIdLong(), amount, message);
			new EconomyNotificationService().sendTransactionNotification(t, event);
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

	/**
	 * Checks the validity of a message that will be added to a transaction to
	 * ensure it is safe and free of formatting characters or other unsightly
	 * things.
	 *
	 * @param message The message to validate.
	 * @return A list of error messages. If this is empty, validation is successful.
	 */
	private List<String> validateMessage(String message) {
		List<String> errors = new ArrayList<>();
		if (message.length() > 127) {
			errors.add("The message is longer than 127 characters.");
		}
		Set<Character> illegalCharactersFound = new HashSet<>();
		Set<Character> allowedOtherChars = Set.of('.', '!', '?', ',', ' ');
		for (char c : message.toCharArray()) {
			if (!Character.isLetterOrDigit(c) && !allowedOtherChars.contains(c) && !illegalCharactersFound.contains(c)) {
				errors.add("The message contains the character `" + c + "` which is not permitted.");
				illegalCharactersFound.add(c);
			}
		}
		return errors;
	}
}
