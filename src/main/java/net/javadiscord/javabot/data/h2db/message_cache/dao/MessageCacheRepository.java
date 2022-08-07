package net.javadiscord.javabot.data.h2db.message_cache.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@RequiredArgsConstructor
public class MessageCacheRepository {
	private final Connection con;

	/**
	 * Inserts a new {@link CachedMessage} object.
	 *
	 * @param message The Message to insert.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean insert(CachedMessage message) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("INSERT INTO message_cache (message_id, author_id, message_content) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		)) {
			stmt.setLong(1, message.getMessageId());
			stmt.setLong(2, message.getAuthorId());
			stmt.setString(3, message.getMessageContent());
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}

	/**
	 * Inserts a {@link List} of {@link CachedMessage} objects.
	 *
	 * @param messages The List to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void insertList(@NotNull List<CachedMessage> messages) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("MERGE INTO message_cache (message_id, author_id, message_content) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		)) {
			con.setAutoCommit(false);
			for (CachedMessage msg : messages) {
				stmt.setLong(1, msg.getMessageId());
				stmt.setLong(2, msg.getAuthorId());
				stmt.setString(3, msg.getMessageContent());
				stmt.executeUpdate();
			}
			con.commit();
		}
	}

	/**
	 * Edit an existing {@link CachedMessage} object.
	 *
	 * @param message The new Message object.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean update(@NotNull CachedMessage message) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("UPDATE message_cache SET message_content = ? WHERE message_id = ?",
				Statement.RETURN_GENERATED_KEYS
		)) {
			stmt.setString(1, message.getMessageContent());
			stmt.setLong(2, message.getMessageId());
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}

	/**
	 * Deletes a single {@link CachedMessage} object from the Message Cache.
	 *
	 * @param messageId The message's id.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean delete(long messageId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("DELETE FROM message_cache WHERE message_id = ?",
				Statement.RETURN_GENERATED_KEYS
		)) {
			stmt.setLong(1, messageId);
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}

	/**
	 * Gets all Messages from the Database.
	 *
	 * @return A {@link List} of {@link CachedMessage}s.
	 * @throws SQLException If anything goes wrong.
	 */
	public List<CachedMessage> getAll() throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM message_cache")) {
			ResultSet rs = s.executeQuery();
			List<CachedMessage> cachedMessages = new ArrayList<>();
			while (rs.next()) {
				cachedMessages.add(this.read(rs));
			}
			return cachedMessages;
		}
	}

	/**
	 * Deletes the given amount of Messages.
	 *
	 * @param amount The amount to delete.
	 * @return If any rows we're affected.
	 * @throws SQLException If anything goes wrong.
	 */
	public boolean delete(int amount) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("DELETE FROM message_cache LIMIT ?",
				Statement.RETURN_GENERATED_KEYS
		)) {
			stmt.setInt(1, amount);
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}

	private CachedMessage read(ResultSet rs) throws SQLException {
		CachedMessage cachedMessage = new CachedMessage();
		cachedMessage.setMessageId(rs.getLong("message_id"));
		cachedMessage.setAuthorId(rs.getLong("author_id"));
		cachedMessage.setMessageContent(rs.getString("message_content"));
		return cachedMessage;
	}
}
