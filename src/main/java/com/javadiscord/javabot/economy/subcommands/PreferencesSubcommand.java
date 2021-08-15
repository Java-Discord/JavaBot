package com.javadiscord.javabot.economy.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.economy.dao.AccountRepository;
import com.javadiscord.javabot.economy.model.AccountPreferences;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This subcommand allows users to update their account preferences. It uses
 * a mapping between a preference name and a {@link PreferenceUpdater} to do the
 * updating logic. To support additional preferences, just add another entry to
 * the map.
 */
public class PreferencesSubcommand implements SlashCommandHandler {

	private interface PreferenceUpdater {
		ReplyAction update(SlashCommandEvent event, AccountPreferences prefs, String value);
	}

	private static final Map<String, PreferenceUpdater> preferenceUpdaters = new HashMap<>();
	static {
		preferenceUpdaters.put("receive_transaction_dms", (event, prefs, value) -> {
			boolean b;
			BiFunction<String, Stream<String>, Boolean> equalsAnyIgnoreCase = (s, strings) -> strings.anyMatch(str -> str.equalsIgnoreCase(s));
			if (equalsAnyIgnoreCase.apply(value, Stream.of("yes", "true", "on", "1"))) {
				b = true;
			} else if (equalsAnyIgnoreCase.apply(value, Stream.of("no", "false", "off", "0"))) {
				b = false;
			} else {
				return Responses.warning(event, "Invalid preference value: " + value + ". Only boolean values are accepted.");
			}
			prefs.setReceiveTransactionDms(b);
			return Responses.success(event, "Preference Updated", "The `receive_transaction_dms` preference has been updated to `" + b + "`.");
		});
	}

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping preferenceNameOption = event.getOption("preference");
		OptionMapping preferenceValueOption = event.getOption("value");
		if (preferenceNameOption == null || preferenceValueOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		try {
			var con = Bot.dataSource.getConnection();
			con.setAutoCommit(false);
			var accountRepository = new AccountRepository(con);
			var account = accountRepository.getAccount(event.getUser().getIdLong());
			if (account == null) {
				return Responses.warning(event, "You don't have an account registered.");
			}
			var prefs = accountRepository.getPreferences(account.getUserId());
			var updater = preferenceUpdaters.get(preferenceNameOption.getAsString());
			if (updater == null) {
				return Responses.warning(
						event,
						"Unsupported Preference",
						String.format(
								"The preference `%s` is not supported. Only the following preferences may be updated: %s",
								preferenceNameOption.getAsString(),
								preferenceUpdaters.keySet().stream().map(s -> "`" + s + "`").collect(Collectors.joining(", "))
						)
				);
			}
			var reply = updater.update(event, prefs, preferenceValueOption.getAsString());
			accountRepository.savePreferences(prefs);
			con.commit();
			con.close();
			return reply;
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An SQL error occurred: " + e.getMessage());
		}
	}
}
