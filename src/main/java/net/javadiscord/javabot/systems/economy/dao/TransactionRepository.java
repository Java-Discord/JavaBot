package net.javadiscord.javabot.systems.economy.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.economy.model.Transaction;
import net.javadiscord.javabot.util.StringResourceCache;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dao class that represents the ECONOMY_TRANSACTION SQL Table.
 */
@RequiredArgsConstructor
public class TransactionRepository {
	private final Connection con;

	/**
	 * Inserts a new {@link Transaction} object.
	 *
	 * @param t The {@link Transaction} object.
	 * @throws SQLException If an error occurs.
	 */
	public void saveNewTransaction(Transaction t) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO economy_transaction (from_user_id, to_user_id, value, message) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		if (t.getFromUserId() != null) {
			stmt.setLong(1, t.getFromUserId());
		} else {
			stmt.setNull(1, Types.BIGINT);
		}
		if (t.getToUserId() != null) {
			stmt.setLong(2, t.getToUserId());
		} else {
			stmt.setNull(2, Types.BIGINT);
		}
		stmt.setLong(3, t.getValue());
		if (t.getMessage() != null) {
			stmt.setString(4, t.getMessage());
		} else {
			stmt.setNull(4, Types.VARCHAR);
		}
		stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			t.setId(rs.getLong(1));
		} else {
			throw new SQLException("Could not obtain generated transaction id.");
		}
	}

	/**
	 * Retrieves a transaction based on the id.
	 *
	 * @param id The transaction's id.
	 * @return A {@link Transaction} object.
	 * @throws SQLException If an error occurs.
	 */
	public Transaction getTransaction(long id) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM economy_transaction WHERE id = ?")) {
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return this.read(rs);
			}
			return null;
		}
	}

	/**
	 * Retrieves the latest transactions of a user.
	 *
	 * @param userId The user's id.
	 * @param count  The count of transactions that should be retrieved.
	 * @return A List with all {@link Transaction}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<Transaction> getLatestTransactions(long userId, int count) throws SQLException {
		String sql = StringResourceCache.load("/economy/sql/find_latest_transactions.sql").replace("/* LIMIT */", "LIMIT " + count);
		try (var stmt = con.prepareStatement(sql)) {
			stmt.setLong(1, userId);
			stmt.setLong(2, userId);
			var rs = stmt.executeQuery();
			List<Transaction> transactions = new ArrayList<>(count);
			while (rs.next()) {
				transactions.add(this.read(rs));
			}
			return transactions;
		}
	}

	private Transaction read(ResultSet rs) throws SQLException {
		Transaction t = new Transaction();
		t.setId(rs.getLong("id"));
		t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		t.setFromUserId(rs.getObject("from_user_id", Long.class));
		t.setToUserId(rs.getObject("to_user_id", Long.class));
		t.setValue(rs.getLong("value"));
		t.setMessage(rs.getString("message"));
		return t;
	}
}
