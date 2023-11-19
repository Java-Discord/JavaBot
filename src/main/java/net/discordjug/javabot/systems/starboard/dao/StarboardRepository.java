package net.discordjug.javabot.systems.starboard.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.systems.starboard.model.StarboardEntry;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the STARBOARD SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class StarboardRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Insertes a single {@link StarboardEntry}.
	 *
	 * @param entry The {@link StarboardEntry}.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(StarboardEntry entry) throws DataAccessException {
		int rows = jdbcTemplate.update("INSERT INTO starboard (original_message_id, guild_id, channel_id, author_id, starboard_message_id) VALUES (?, ?, ?, ?, ?)",
				entry.getOriginalMessageId(), entry.getGuildId(), entry.getChannelId(), entry.getAuthorId(), entry.getStarboardMessageId());
		if (rows == 0) throw new DataAccessException("Starboard Entry was not inserted.") {};
		log.info("Inserted new Starboard-Entry: {}", entry);
	}

	/**
	 * Deletes a single {@link StarboardEntry} based on the message id.
	 *
	 * @param messageId The entries' message id.
	 * @throws SQLException If an error occurs.
	 */
	public void delete(long messageId) throws DataAccessException {
		jdbcTemplate.update("""
				DELETE FROM starboard
				WHERE original_message_id = ?""",
				messageId);
	}

	/**
	 * Gets a {@link StarboardEntry} by its message id.
	 *
	 * @param messageId The entries' message id.
	 * @return The {@link StarboardEntry} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<StarboardEntry> getEntryByMessageId(long messageId) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM starboard WHERE original_message_id = ?", (rs, row)->this.read(rs),
					messageId));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets a {@link StarboardEntry} by its starboard message id.
	 *
	 * @param starboardMessageId The entries' starboard message id.
	 * @return The {@link StarboardEntry} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<StarboardEntry> getEntryByStarboardMessageId(long starboardMessageId) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM starboard WHERE starboard_message_id = ?", (rs, row)->this.read(rs),
					starboardMessageId));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Retrieves all {@link StarboardEntry}s by the given guild.
	 *
	 * @param guildId The current guild's id.
	 * @return A {@link List} containing all {@link StarboardEntry}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<StarboardEntry> getAllStarboardEntries(long guildId) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM starboard WHERE guild_id = ?", (rs, row)->this.read(rs),
				guildId);
	}

	private @NotNull StarboardEntry read(@NotNull ResultSet rs) throws SQLException {
		StarboardEntry entry = new StarboardEntry();
		entry.setOriginalMessageId(rs.getLong("original_message_id"));
		entry.setGuildId(rs.getLong("guild_id"));
		entry.setChannelId(rs.getLong("channel_id"));
		entry.setAuthorId(rs.getLong("author_id"));
		entry.setStarboardMessageId(rs.getLong("starboard_message_id"));
		return entry;
	}
}
