package net.javadiscord.javabot.systems.moderation.warn.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for interacting with the set of {@link Warn} objects.
 */
@RequiredArgsConstructor
public class WarnRepository {
	private final Connection con;

	/**
	 * Inserts a new warn into the database.
	 *
	 * @param warn The warn to save.
	 * @return The warn that was saved.
	 * @throws SQLException If an error occurs.
	 */
	public Warn insert(Warn warn) throws SQLException {
		try (var s = con.prepareStatement(
				"INSERT INTO warn (user_id, warned_by, severity, severity_weight, reason) VALUES (?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		)) {
			s.setLong(1, warn.getUserId());
			s.setLong(2, warn.getWarnedBy());
			s.setString(3, warn.getSeverity());
			s.setInt(4, warn.getSeverityWeight());
			s.setString(5, warn.getReason());
			s.executeUpdate();
			var rs = s.getGeneratedKeys();
			if (!rs.next()) throw new SQLException("No generated keys returned.");
			long id = rs.getLong(1);
			return findById(id).orElseThrow();
		}
	}

	/**
	 * Finds a warn by its id.
	 *
	 * @param id The id of the warn.
	 * @return The warn, if it was found.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<Warn> findById(long id) throws SQLException {
		Warn warn = null;
		try (var s = con.prepareStatement("SELECT * FROM warn WHERE id = ?")) {
			s.setLong(1, id);
			var rs = s.executeQuery();
			if (rs.next()) {
				warn = read(rs);
			}
			rs.close();
		}
		return Optional.ofNullable(warn);
	}

	/**
	 * Gets the total severity weight of all warns for the given user, which
	 * were created after the given cutoff, and haven't been discarded.
	 *
	 * @param userId The id of the user.
	 * @param cutoff The time after which to look for warns.
	 * @return The total weight of all warn severities.
	 * @throws SQLException If an error occurs.
	 */
	public int getTotalSeverityWeight(long userId, LocalDateTime cutoff) throws SQLException {
		try (var s = con.prepareStatement("SELECT SUM(severity_weight) FROM warn WHERE user_id = ? AND discarded = FALSE AND created_at > ?")) {
			s.setLong(1, userId);
			s.setTimestamp(2, Timestamp.valueOf(cutoff));
			var rs = s.executeQuery();
			int sum = 0;
			if (rs.next()) {
				sum = rs.getInt(1);
			}
			rs.close();
			return sum;
		}
	}

	/**
	 * Discards all warnings that have been issued to a given user.
	 *
	 * @param userId The id of the user to discard warnings for.
	 * @throws SQLException If an error occurs.
	 */
	public void discardAll(long userId) throws SQLException {
		try (var s = con.prepareStatement("""
				UPDATE warn SET discarded = TRUE
				WHERE user_id = ?""")) {
			s.setLong(1, userId);
			s.executeUpdate();
		}
	}

	/**
	 * Discards the warning with the corresponding id.
	 *
	 * @param id The id of the Warn to discard.
	 * @throws SQLException If an error occurs.
	 */
	public void discardById(long id) throws SQLException {
		try (var s = con.prepareStatement("""
				UPDATE warn SET discarded = TRUE
				WHERE id = ?""")) {
			s.setLong(1, id);
			s.executeUpdate();
		}
	}

	/**
	 * Reads the given {@link ResultSet} and constructs a new {@link Warn} object.
	 *
	 * @param rs The ResultSet
	 * @throws SQLException If an error occurs.
	 */
	private Warn read(ResultSet rs) throws SQLException {
		Warn warn = new Warn();
		warn.setId(rs.getLong("id"));
		warn.setUserId(rs.getLong("user_id"));
		warn.setWarnedBy(rs.getLong("warned_by"));
		warn.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		warn.setSeverity(rs.getString("severity"));
		warn.setSeverityWeight(rs.getInt("severity_weight"));
		warn.setReason(rs.getString("reason"));
		warn.setDiscarded(rs.getBoolean("discarded"));
		return warn;
	}

	/**
	 * Gets all warns for the given user, which
	 * were created after the given cutoff, and haven't been discarded.
	 *
	 * @param userId The id of the user.
	 * @param cutoff The time after which to look for warns.
	 * @return A List with all Warns.
	 * @throws SQLException If an error occurs.
	 */
	public List<Warn> getWarnsByUserId(long userId, LocalDateTime cutoff) {
		List<Warn> warns = new ArrayList<>();
		try (var s = con.prepareStatement("SELECT * FROM warn WHERE user_id = ? AND discarded = FALSE AND created_at > ?")) {
			s.setLong(1, userId);
			s.setTimestamp(2, Timestamp.valueOf(cutoff));
			var rs = s.executeQuery();
			while (rs.next()) warns.add(read(rs));
			rs.close();
			return warns;
		} catch (SQLException e) {
			e.printStackTrace();
			return List.of();
		}
	}
}
