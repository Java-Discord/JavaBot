package net.discordjug.javabot.systems.moderation.warn.dao;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.systems.moderation.warn.model.Warn;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO for interacting with the set of {@link Warn} objects.
 */
@RequiredArgsConstructor
@Repository
public class WarnRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a new warn into the database.
	 *
	 * @param warn The warn to save.
	 * @return The warn that was saved.
	 * @throws SQLException If an error occurs.
	 */
	public Warn insert(@NotNull Warn warn) throws DataAccessException {
		SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
				.withTableName("warn")
				.usingColumns("user_id","warned_by","severity","severity_weight","reason")
				.usingGeneratedKeyColumns("id");
				Number key = simpleJdbcInsert.executeAndReturnKey(Map.of(
							"user_id",warn.getUserId(),
							"warned_by",warn.getWarnedBy(),
							"severity",warn.getSeverity(),
							"severity_weight",warn.getSeverityWeight(),
							"reason",warn.getReason())
						);
				long id = key.longValue();
				return findById(id).orElseThrow();
	}

	/**
	 * Finds a warn by its id.
	 *
	 * @param id The id of the warn.
	 * @return The warn, if it was found.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<Warn> findById(long id) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM warn WHERE id = ?", (rs, row)->read(rs),id));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	/**
	 * Discards all warnings that have been issued to a given user after a given timestamp.
	 *
	 * @param userId The id of the user to discard warnings for.
	 * @param cutoff timestamp specifying which warns should be discarded. Only warns after the cutoff are discarded.
	 * @throws SQLException If an error occurs.
	 */
	public void discardAll(long userId, LocalDateTime cutoff) throws DataAccessException {
		jdbcTemplate.update("""
				UPDATE warn SET discarded = TRUE
				WHERE user_id = ?
				AND created_at > ?""",
				userId, Timestamp.valueOf(cutoff));
	}

	/**
	 * Discards the warning with the corresponding id.
	 *
	 * @param id The id of the Warn to discard.
	 * @throws SQLException If an error occurs.
	 */
	public void discardById(long id) throws DataAccessException {
		jdbcTemplate.update("""
				UPDATE warn SET discarded = TRUE
				WHERE id = ?""",
				id);
	}

	/**
	 * Reads the given {@link ResultSet} and constructs a new {@link Warn} object.
	 *
	 * @param rs The ResultSet
	 * @return The {@link Warn} object.
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
	 */
	public List<Warn> getActiveWarnsByUserId(long userId, LocalDateTime cutoff) {
		return jdbcTemplate.query("SELECT * FROM warn WHERE user_id = ? AND discarded = FALSE AND created_at > ? ORDER BY created_at DESC",(rs, row)->this.read(rs),
				userId, Timestamp.valueOf(cutoff));
	}

	public List<Warn> getAllWarnsByUserId(long userId) {
		return jdbcTemplate.query("SELECT * FROM warn WHERE user_id = ?",(rs, row) -> this.read(rs),
				userId);
	}
}
