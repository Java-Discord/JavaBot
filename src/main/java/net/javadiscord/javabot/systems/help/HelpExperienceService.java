package net.javadiscord.javabot.systems.help;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.systems.help.model.HelpTransactionMessage;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * Service class that handles Help Experience Transactions.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class HelpExperienceService {
	private final DataSource dataSource;
	private final BotConfig botConfig;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;

	/**
	 * Creates a new Help Account if none exists.
	 *
	 * @param userId The user's id.
	 * @return An {@link HelpAccount} object.
	 * @throws DataAccessException If an error occurs.
	 */
	@Transactional
	public HelpAccount getOrCreateAccount(long userId) throws DataAccessException {
		HelpAccount account;
			Optional<HelpAccount> optional = helpAccountRepository.getByUserId(userId);
			if (optional.isPresent()) {
				account = optional.get();
			} else {
				account = new HelpAccount(botConfig);
				account.setUserId(userId);
				account.setExperience(0);
				helpAccountRepository.insert(account);
			}
			return account;
	}

	/**
	 * Returns the specified amount of {@link HelpAccount}s, sorted by their experience.
	 *
	 * @param amount The amount to retrieve.
	 * @param page The page to get.
	 * @return A {@link List} of {@link HelpAccount}s.
	 */
	public List<HelpAccount> getTopAccounts(int amount, int page) {
		try{
			return helpAccountRepository.getAccounts(page, amount);
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return Collections.emptyList();
		}
	}

	/**
	 * Gets all recent help transactions from a user.
	 *
	 * @param userId The user's id.
	 * @param count  The count of transactions to retrieve.
	 * @return A {@link List} with all transactions.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<HelpTransaction> getRecentTransactions(long userId, int count) throws DataAccessException {
		List<HelpTransaction> transactions = helpTransactionRepository.getTransactions(userId, count);
		return transactions;
	}

	/**
	 * Performs a single transaction.
	 *
	 * @param recipient   The recipient's user id.
	 * @param value      The transaction's value.
	 * @param message    The transaction's message.
	 * @param guild      The current guild.
	 * @return A {@link HelpTransaction} object.
	 * @throws DataAccessException If an error occurs.
	 */
	@Transactional
	public HelpTransaction performTransaction(long recipient, double value, HelpTransactionMessage message, Guild guild) throws DataAccessException {
		if (value == 0) {
			log.error("Cannot make zero-value transactions");
			return null;
		}
		HelpTransaction transaction = new HelpTransaction();
		transaction.setRecipient(recipient);
		transaction.setWeight(value);
		transaction.setMessageType(message.ordinal());
		HelpAccount account = this.getOrCreateAccount(recipient);
		account.updateExperience(value);
		helpAccountRepository.update(account);
		transaction = helpTransactionRepository.save(transaction);
		this.checkExperienceRoles(guild, account);
		return helpTransactionRepository.getTransaction(transaction.getId()).orElse(null);
	}

	private void checkExperienceRoles(@NotNull Guild guild, @NotNull HelpAccount account) {
		guild.retrieveMemberById(account.getUserId()).queue(member ->
				botConfig.get(guild).getHelpConfig().getExperienceRoles().forEach((key, value) -> {
					Pair<Role, Double> role = account.getCurrentExperienceGoal(guild);
					if (role.first() == null) return;
					if (key.equals(role.first().getIdLong())) {
						guild.addRoleToMember(member, role.first()).queue();
					} else {
						guild.removeRoleFromMember(member, guild.getRoleById(key)).queue();
					}
		}), e -> {});
	}
}
