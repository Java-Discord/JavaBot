package net.javadiscord.javabot.systems.qotw.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@RequiredArgsConstructor
@Repository
public class QuestionPointsRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Returns a {@link QOTWAccount} based on the given user Id.
	 *
	 * @param userId The discord Id of the user.
	 * @param startDate The earliest date where points are counted
	 * @return The {@link QOTWAccount} object.
	 * @throws DataAccessException If an error occurs.
	 */
	public Optional<QOTWAccount> getByUserId(long userId, LocalDate startDate) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT SUM(points) AS points FROM qotw_points WHERE user_id = ? AND obtained_at >= ?",
					(rs, row)->{
						QOTWAccount acc=new QOTWAccount();
						acc.setUserId(userId);
						acc.setPoints(rs.getLong(1));
						return acc;
					},
					userId, startDate));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the number points given to a user at a certain date.
	 *
	 * @param userId the ID of the user
	 * @param date the date where the points are given to that user
	 * @return the number of points the user obtained at the given date
	 */
	public int getPointsAtDate(long userId, LocalDate date) {
		try {
			return jdbcTemplate.queryForObject("SELECT SUM(points) FROM qotw_points WHERE user_id = ? AND obtained_at >= ?",
					(rs, row) -> rs.getInt(1),
					userId, date);
		}catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	/**
	 * Sets the points of a user at a certain date.
	 *
	 * @param userId the id of the user to set to points of
	 * @param date the date when the points should be marked as set
	 * @param points the (new) number of points the user got at that date
	 * @return {@code true} if a change was made, else {@code false}
	 */
	public boolean setPointsAtDate(long userId, LocalDate date, long points) {
		return jdbcTemplate.update("MERGE INTO qotw_points (user_id,obtained_at,points) KEY(user_id,obtained_at) VALUES (?,?,?)",
				userId, date, points) > 0;
	}

	/**
	 * Gets all {@link QOTWAccount} and sorts them by their points.
	 *
	 * @param startDate the minimum date points are considered
	 * @return A {@link List} that contains all {@link QOTWAccount}s sorted by their points.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<QOTWAccount> sortByPoints(LocalDate startDate) throws DataAccessException {
		return jdbcTemplate.query("SELECT user_id, SUM(points) FROM qotw_points WHERE obtained_at >= ? GROUP BY user_id ORDER BY SUM(points) DESC", (rs, row)->this.read(rs), startDate);
	}

	/**
	 * Gets a specified amount of {@link QOTWAccount}s.
	 *
	 * @param startDate the minimum date points are considered
	 * @param page The page.
	 * @param size The amount of {@link QOTWAccount}s to return.
	 * @return A {@link List} containing the specified amount of {@link QOTWAccount}s.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<QOTWAccount> getTopAccounts(LocalDate startDate, int page, int size) throws DataAccessException {
		return jdbcTemplate.query("SELECT user_id, SUM(points) FROM qotw_points WHERE startDate >= ? AND points > 0  GROUP BY user_id ORDER BY SUM(points) DESC LIMIT ? OFFSET ?", (rs,row)->this.read(rs),
				startDate, size, Math.max(0, (page * size) - size));
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
		account.setUserId(rs.getLong(1));
		account.setPoints(rs.getLong(2));
		return account;
	}
}
