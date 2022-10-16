package net.javadiscord.javabot.systems.qotw.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class QuestionPointsRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a new {@link QOTWAccount} if none exists.
	 *
	 * @param account The account to insert.
	 * @throws DataAccessException If an error occurs.
	 */
	public void insert(QOTWAccount account) throws DataAccessException {
		int rows = jdbcTemplate.update("INSERT INTO qotw_points (user_id, points) VALUES (?, ?)",
				account.getUserId(),account.getPoints());
		if (rows == 0) throw new DataAccessException("User was not inserted.") {};
		log.info("Inserted new QOTW-Account: {}", account);
	}

	/**
	 * Returns a {@link QOTWAccount} based on the given user Id.
	 *
	 * @param userId The discord Id of the user.
	 * @return The {@link QOTWAccount} object.
	 * @throws DataAccessException If an error occurs.
	 */
	public Optional<QOTWAccount> getByUserId(long userId) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM qotw_points WHERE user_id = ?", (rs, row)->this.read(rs),userId));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Updates a single QOTW Account.
	 *
	 * @param account The updated QOTW Account.
	 * @return Whether the update affected rows.
	 * @throws DataAccessException If an error occurs.
	 */
	public boolean update(@NotNull QOTWAccount account) throws DataAccessException {
		return jdbcTemplate.update("UPDATE qotw_points SET points = ? WHERE user_id = ?",
				account.getPoints(), account.getUserId()) > 0;
	}

	/**
	 * Gets all {@link QOTWAccount} and sorts them by their points.
	 *
	 * @return A {@link List} that contains all {@link QOTWAccount}s sorted by their points.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<QOTWAccount> sortByPoints() throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM qotw_points ORDER BY points DESC", (rs, row)->this.read(rs));
	}

	/**
	 * Gets a specified amount of {@link QOTWAccount}s.
	 *
	 * @param page    The page.
	 * @param size    The amount of {@link QOTWAccount}s to return.
	 * @return A {@link List} containing the specified amount of {@link QOTWAccount}s.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<QOTWAccount> getTopAccounts(int page, int size) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM qotw_points WHERE points > 0 ORDER BY points DESC LIMIT ? OFFSET ?", (rs,row)->this.read(rs),
				size, Math.max(0, (page * size) - size));
	}

	/**
	 * Reads a {@link ResultSet} and returns a new {@link QOTWAccount} object.
	 *
	 * @param rs The query's ResultSet.
	 * @return The {@link QOTWAccount} object.
	 * @throws SQLException If an error occurs.
	 */
	private QOTWAccount read(ResultSet rs) throws SQLException {
		QOTWAccount account = new QOTWAccount();
		account.setUserId(rs.getLong("user_id"));
		account.setPoints(rs.getLong("points"));
		return account;
	}
}
