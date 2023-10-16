package net.javadiscord.javabot.systems.help.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.util.Pair;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dao class that represents the HELP_TRANSACTION SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class HelpTransactionRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a new {@link HelpTransaction}.
	 *
	 * @param transaction The transaction that should be inserted.
	 * @return The inserted {@link HelpTransaction}.
	 * @throws SQLException If an error occurs.
	 */
	public HelpTransaction save(HelpTransaction transaction) throws DataAccessException {
		SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
		.withTableName("help_transaction")
		.usingColumns("recipient","weight","messageType", "channel")
		.usingGeneratedKeyColumns("id");
		Number key = simpleJdbcInsert.executeAndReturnKey(Map.of(
					"recipient",transaction.getRecipient(),
					"weight",transaction.getWeight(),
					"messageType",transaction.getMessageType(),
					"channel",transaction.getChannelId())
				);
		transaction.setId(key.longValue());
		log.info("Inserted new Help Transaction: {}", transaction);
		return transaction;
	}

	/**
	 * Retrieves a transaction based on the id.
	 *
	 * @param id The transaction's id.
	 * @return A {@link HelpTransaction} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<HelpTransaction> getTransaction(long id) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM help_transaction WHERE id = ?", (rs, row)->this.read(rs), id));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
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
	public List<HelpTransaction> getTransactions(long userId, int count) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM help_transaction WHERE recipient = ? ORDER BY created_at DESC LIMIT ?", (rs, rowNumber) -> this.read(rs), userId, count);
	}

	private HelpTransaction read(ResultSet rs) throws SQLException {
		HelpTransaction transaction = new HelpTransaction();
		transaction.setId(rs.getLong("id"));
		transaction.setRecipient(rs.getLong("recipient"));
		transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		transaction.setWeight(rs.getDouble("weight"));
		transaction.setMessageType(rs.getInt("messageType"));
		transaction.setChannelId(rs.getLong("channel"));
		return transaction;
	}

	/**
	 * Gets the total earned XP of a user since a specific timestamp grouped by months.
	 * @param userId the user to get XP from
	 * @param start the start timestamp
	 * @return a list consisting of month, year and the total XP earned that month
	 */
	public List<Pair<MonthInYear, Double>> getTotalTransactionWeightByMonth(long userId, LocalDateTime start) {
		return jdbcTemplate.query("SELECT SUM(weight) AS total, EXTRACT(MONTH FROM created_at) AS m, EXTRACT(YEAR FROM created_at) AS y FROM help_transaction WHERE recipient = ? AND created_at >= ? GROUP BY m, y ORDER BY y ASC, m ASC", 
				(rs, row)-> new Pair<>(new MonthInYear(rs.getInt("m"), rs.getInt("y")), rs.getDouble("total")), 
				userId, start);
	}
	
	/**
	 * Gets the total earned XP of a user since a specific timestamp grouped by months and users.
	 * @param start the start timestamp
	 * @return a list consisting of month, year, user and the total XP earned that month
	 */
	public List<Pair<MonthInYear, HelpAccount>> getTotalTransactionWeightByMonthAndUsers(LocalDateTime start) {
		return jdbcTemplate.query("SELECT SUM(weight) AS total, EXTRACT(MONTH FROM created_at) AS m, EXTRACT(YEAR FROM created_at) AS y, recipient FROM help_transaction WHERE created_at >= ? GROUP BY m, y, recipient ORDER BY y ASC, m ASC, total DESC", 
				(rs, row)-> new Pair<>(new MonthInYear(rs.getInt("m"), rs.getInt("y")), new HelpAccount(rs.getLong("recipient"), rs.getDouble("total"))),
				start);
	}
	
	/**
	 * Checks whether a transaction with a specific recipient exists in a specific channel.
	 * @param recipient The ID of the recipient
	 * @param channelId The ID of the channel
	 * @return {@code true} if it exists, else {@code false}
	 */
	public boolean existsTransactionWithRecipientInChannel(long recipient, long channelId) {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM help_transaction WHERE recipient=? AND channel = ?",
				Integer.class,
				recipient, channelId) > 0;
	}

	/**
	 * Stores a given month in a specific year.
	 * @param month the month in the year.
	 * @param year the year.
	 */
	public record MonthInYear(int month, int year) {}
}
