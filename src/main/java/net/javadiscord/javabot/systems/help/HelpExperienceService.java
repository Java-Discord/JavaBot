package net.javadiscord.javabot.systems.help;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.systems.help.model.HelpTransactionMessage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service class that handles Help Experience Transactions.
 */
@Slf4j
@RequiredArgsConstructor
public class HelpExperienceService {
	private final HikariDataSource dataSource;

	/**
	 * Creates a new Help Account if none exists.
	 *
	 * @param userId The user's id.
	 * @return An {@link HelpAccount} object.
	 * @throws SQLException If an error occurs.
	 */
	public HelpAccount getOrCreateAccount(long userId) throws SQLException {
		HelpAccount account;
		try (Connection con = this.dataSource.getConnection()) {
			con.setAutoCommit(false);
			HelpAccountRepository accountRepository = new HelpAccountRepository(con);
			Optional<HelpAccount> optional = accountRepository.getByUserId(userId);
			if (optional.isPresent()) {
				account = optional.get();
			} else {
				account = new HelpAccount();
				account.setUserId(userId);
				account.setExperience(0);
				accountRepository.insert(account);
			}
			con.commit();
			return account;
		}
	}

	/**
	 * Gets all recent help transactions from a user.
	 *
	 * @param userId The user's id.
	 * @param count  The count of transactions to retrieve.
	 * @return A {@link List} with all transactions.
	 * @throws SQLException If an error occurs.
	 */
	public List<HelpTransaction> getRecentTransactions(long userId, int count) throws SQLException {
		try (Connection con = this.dataSource.getConnection()) {
			con.setReadOnly(true);
			HelpTransactionRepository repo = new HelpTransactionRepository(con);
			List<HelpTransaction> transactions = repo.getTransactions(userId, count);
			con.close();
			return transactions;
		}
	}

	/**
	 * Performs a single transaction.
	 *
	 * @param recipient   The recipient's user id.
	 * @param value      The transaction's value.
	 * @param message    The transaction's message.
	 * @return A {@link HelpTransaction} object.
	 * @throws SQLException If an error occurs.
	 */
	public HelpTransaction performTransaction(long recipient, double value, HelpTransactionMessage message) throws SQLException {
		if (value == 0) {
			log.error("Cannot make zero-value transactions");
			return null;
		}
		HelpTransaction transaction = new HelpTransaction();
		transaction.setRecipient(recipient);
		transaction.setValue(value);
		transaction.setMessageType(message.ordinal());
		try (Connection con = dataSource.getConnection()) {
			con.setAutoCommit(false);
			HelpAccountRepository accountRepository = new HelpAccountRepository(con);
			HelpTransactionRepository transactionRepository = new HelpTransactionRepository(con);
			HelpAccount account = this.getOrCreateAccount(recipient);
			account.updateExperience(value);
			accountRepository.update(account);
			transaction = transactionRepository.save(transaction);
			con.commit();
			return transactionRepository.getTransaction(transaction.getId()).orElse(null);
		}
	}
}
