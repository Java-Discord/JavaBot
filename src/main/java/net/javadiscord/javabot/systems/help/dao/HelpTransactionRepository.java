package net.javadiscord.javabot.systems.help.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the HELP_TRANSACTION SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
public class HelpTransactionRepository {
	private final Connection con;

	/**
	 * Inserts a new {@link HelpTransaction}.
	 *
	 * @param transaction The transaction that should be inserted.
	 * @return The inserted {@link HelpTransaction}.
	 * @throws SQLException If an error occurs.
	 */
	public HelpTransaction save(HelpTransaction transaction) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("INSERT INTO help_transaction (recipient, value, messageType) VALUES ( ?, ?, ? )")) {
			s.setLong(1, transaction.getRecipient());
			s.setDouble(2, transaction.getValue());
			if (transaction.getMessage() != null) {
				s.setInt(3, transaction.getMessageType());
			}
			s.executeUpdate();
			ResultSet rs = s.getGeneratedKeys();
			if (rs.next()) {
				transaction.setId(rs.getLong(1));
			}
			log.info("Inserted new Help Transaction: {}", transaction);
			return transaction;
		}
	}

	/**
	 * Retrieves a transaction based on the id.
	 *
	 * @param id The transaction's id.
	 * @return A {@link HelpTransaction} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<HelpTransaction> getTransaction(long id) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM help_transaction WHERE id = ?")) {
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			HelpTransaction transaction = null;
			if (rs.next()) {
				transaction = this.read(rs);
			}
			return Optional.ofNullable(transaction);
		}
	}

	/**
	 * Retrieves the latest transactions of a user.
	 *
	 * @param userId The user's id.
	 * @param count  The count of transactions that should be retrieved.
	 * @return A List with all {@link HelpTransaction}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<HelpTransaction> getTransactions(long userId, int count) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM help_transaction WHERE recipient = ? ORDER BY created_at DESC LIMIT ?")) {
			s.setLong(1, userId);
			s.setInt(2, count);
			ResultSet rs = s.executeQuery();
			List<HelpTransaction> transactions = new ArrayList<>(count);
			while (rs.next()) {
				transactions.add(this.read(rs));
			}
			return transactions;
		}
	}

	private HelpTransaction read(ResultSet rs) throws SQLException {
		HelpTransaction transaction = new HelpTransaction();
		transaction.setId(rs.getLong("id"));
		transaction.setRecipient(rs.getLong("recipient"));
		transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		transaction.setValue(rs.getDouble("value"));
		transaction.setMessageType(rs.getInt("messageType"));
		return transaction;
	}
}
