package net.javadiscord.javabot.systems.qotw.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
// TODO: Implement DatabaseRepository
public class QuestionPointsRepository {
	private final Connection con;

	/**
	 * Inserts a new {@link QOTWAccount} if none exists.
	 *
	 * @param account The account to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(QOTWAccount account) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("INSERT INTO qotw_points (user_id, points) VALUES (?, ?)",
				Statement.RETURN_GENERATED_KEYS
		)) {
			stmt.setLong(1, account.getUserId());
			stmt.setLong(2, account.getPoints());
			int rows = stmt.executeUpdate();
			if (rows == 0) throw new SQLException("User was not inserted.");
			log.info("Inserted new QOTW-Account: {}", account);
		}
	}

	/**
	 * Returns a {@link QOTWAccount} based on the given user Id.
	 *
	 * @param userId The discord Id of the user.
	 * @return The {@link QOTWAccount} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWAccount> getByUserId(long userId) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_points WHERE user_id = ?")) {
			s.setLong(1, userId);
			QOTWAccount account = null;
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				account = read(rs);
			}
			return Optional.ofNullable(account);
		}
	}

	/**
	 * Updates a single QOTW Account.
	 *
	 * @param account The updated QOTW Account.
	 * @return Whether the update affected rows.
	 * @throws SQLException If an error occurs.
	 */
	public boolean update(@NotNull QOTWAccount account) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("UPDATE qotw_points SET points = ? WHERE user_id = ?")) {
			s.setLong(1, account.getPoints());
			s.setLong(2, account.getUserId());
			return s.executeUpdate() > 0;
		}
	}

	/**
	 * Gets all {@link QOTWAccount} and sorts them by their points.
	 *
	 * @return A {@link List} that contains all {@link QOTWAccount}s sorted by their points.
	 * @throws SQLException If an error occurs.
	 */
	public List<QOTWAccount> sortByPoints() throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_points ORDER BY points DESC")) {
			ResultSet rs = s.executeQuery();
			List<QOTWAccount> accounts = new ArrayList<>();
			while (rs.next()) {
				accounts.add(read(rs));
			}
			return accounts;
		}
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
