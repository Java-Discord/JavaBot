package net.javadiscord.javabot.data.h2db.message_cache.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@RequiredArgsConstructor
@Repository
public class MessageCacheRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a new {@link CachedMessage} object.
	 *
	 * @param message The Message to insert.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean insert(CachedMessage message) throws DataAccessException {
		int rows = jdbcTemplate.update(
				"INSERT INTO message_cache (message_id, author_id, message_content) VALUES (?, ?, ?)",
				message.getMessageId(), message.getAuthorId(), message.getMessageContent());
		return rows > 0;
	}

	/**
	 * Inserts a {@link List} of {@link CachedMessage} objects.
	 *
	 * @param messages The List to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void insertList(@NotNull List<CachedMessage> messages) throws DataAccessException {
		jdbcTemplate.batchUpdate("MERGE INTO message_cache (message_id, author_id, message_content) VALUES (?, ?, ?)",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement stmt, int i) throws SQLException {
						CachedMessage msg = messages.get(i);
						stmt.setLong(1, msg.getMessageId());
						stmt.setLong(2, msg.getAuthorId());
						stmt.setString(3, msg.getMessageContent());
						stmt.executeUpdate();
					}

					@Override
					public int getBatchSize() {
						return messages.size();
					}
				});
	}

	/**
	 * Edit an existing {@link CachedMessage} object.
	 *
	 * @param message The new Message object.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean update(@NotNull CachedMessage message) throws DataAccessException {
		int rows = jdbcTemplate.update("UPDATE message_cache SET message_content = ? WHERE message_id = ?",
				message.getMessageContent(), message.getMessageId());
		return rows > 0;
	}

	/**
	 * Deletes a single {@link CachedMessage} object from the Message Cache.
	 *
	 * @param messageId The message's id.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean delete(long messageId) throws DataAccessException {
		int rows = jdbcTemplate.update("DELETE FROM message_cache WHERE message_id = ?", messageId);
		return rows > 0;
	}

	/**
	 * Gets all Messages from the Database.
	 *
	 * @return A {@link List} of {@link CachedMessage}s.
	 * @throws SQLException If anything goes wrong.
	 */
	public List<CachedMessage> getAll() throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM message_cache",(RowMapper<CachedMessage>) (rs, rowNum) -> this.read(rs));
	}

	/**
	 * Deletes the given amount of Messages.
	 *
	 * @param amount The amount to delete.
	 * @return If any rows we're affected.
	 * @throws SQLException If anything goes wrong.
	 */
	public boolean delete(int amount) throws DataAccessException {
		int rows = jdbcTemplate.update("DELETE FROM message_cache LIMIT ?", amount);
		return rows > 0;
	}

	private CachedMessage read(ResultSet rs) throws SQLException {
		CachedMessage cachedMessage = new CachedMessage();
		cachedMessage.setMessageId(rs.getLong("message_id"));
		cachedMessage.setAuthorId(rs.getLong("author_id"));
		cachedMessage.setMessageContent(rs.getString("message_content"));
		return cachedMessage;
	}
}
