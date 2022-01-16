package net.javadiscord.javabot.systems.qotw.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class QuestionPointsRepository {
	private final Connection con;

	/**
	 * Inserts a new {@link QOTWAccount} if none exists.
	 * @param account The account to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(QOTWAccount account) throws SQLException {
			PreparedStatement stmt = con.prepareStatement("INSERT INTO qotw_points (user_id, points) VALUES (?, ?)",
					Statement.RETURN_GENERATED_KEYS
			);
			stmt.setLong(1, account.getUserId());
			stmt.setLong(2, account.getPoints());
			int rows = stmt.executeUpdate();
			if (rows == 0) throw new SQLException("User was not inserted.");
			stmt.close();
	}

	/**
	 * Returns a {@link QOTWAccount} based on the given user Id.
	 * @param userId The discord Id of the user.
	 * @throws SQLException If an error occurs.
	 */
	public QOTWAccount getAccountByUserId(long userId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_points WHERE user_id = ?");
		s.setLong(1, userId);
		var rs = s.executeQuery();
		if (rs.next()) {
			return read(rs);
		} else {
			QOTWAccount account = new QOTWAccount();
			account.setUserId(userId);
			account.setPoints(0);
			insert(account);
			return account;
		}
	}

	/**
	 * Updates a single user's QOTW-Points.
	 * @param userId The discord Id of the user.
	 * @param points The points that should be set.
	 * @throws SQLException If an error occurs.
	 */
	public QOTWAccount update(long userId, long points) throws SQLException {
		createAccountIfUserHasNone(userId);
		PreparedStatement s = con.prepareStatement("UPDATE qotw_points SET points = ? WHERE user_id = ?");
		s.setLong(1, points);
		s.setLong(2, userId);
		s.executeUpdate();
		return getAccountByUserId(userId);
	}

	/**
	 * Increments a single user's QOTW-Points.
	 * @param userId The discord Id of the user.
	 * @throws SQLException If an error occurs.
	 * @return The total points after the update.
	 */
	public long increment(long userId) throws SQLException {
		createAccountIfUserHasNone(userId);
		var points = getAccountByUserId(userId).getPoints() + 1;
		update(userId, points);
		return points;
	}

	/**
	 * Gets all {@link QOTWAccount} and sorts them by their points.
	 * @throws SQLException If an error occurs.
	 */
	public List<QOTWAccount> getAllAccountsSortedByPoints() throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_points ORDER BY points DESC");
		var rs = s.executeQuery();
		List<QOTWAccount> accounts = new ArrayList<>();
		while (rs.next()) {
			accounts.add(read(rs));
		}
		return accounts;
	}

	/**
	 * Creates a new {@link QOTWAccount} for the given user if they have none.
	 * @param userId The discord Id of the user.
	 * @throws SQLException If an error occurs.
	 */
	private void createAccountIfUserHasNone(long userId) throws SQLException {
		if (getAccountByUserId(userId) == null) {
			QOTWAccount account = new QOTWAccount();
			account.setUserId(userId);
			account.setPoints(0);
			insert(account);
		}
	}

	/**
	 * Reads a {@link ResultSet} and returns a new {@link QOTWAccount} object.
	 * @param rs The query's ResultSet.
	 * @throws SQLException If an error occurs.
	 */
	private QOTWAccount read(ResultSet rs) throws SQLException {
		QOTWAccount account = new QOTWAccount();
		account.setUserId(rs.getLong("user_id"));
		account.setPoints(rs.getLong("points"));
		return account;
	}
}
