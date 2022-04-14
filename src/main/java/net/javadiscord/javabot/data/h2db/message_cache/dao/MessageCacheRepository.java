package net.javadiscord.javabot.data.h2db.message_cache.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@Slf4j
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
		PreparedStatement stmt = con.prepareStatement("INSERT INTO message_cache (message_id, author_id, message_content) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, message.getMessageId());
		stmt.setLong(2, message.getAuthorId());
		stmt.setString(3, message.getMessageContent());
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Inserts a {@link List} of {@link CachedMessage} objects..
	 *
	 * @param messages The List to insert.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean insertList(List<CachedMessage> messages) throws SQLException {
		StringBuilder statementString = new StringBuilder("INSERT INTO message_cache (message_id, author_id, message_content) VALUES");
		for (CachedMessage msg:messages) {
			statementString.append(String.format(" (%s, %s, '%s'),", msg.getMessageId(), msg.getAuthorId(), msg.getMessageContent()));
		}
		statementString.deleteCharAt(statementString.toString().length() - 1).append(";");
		PreparedStatement stmt = con.prepareStatement(statementString.toString(),
				Statement.RETURN_GENERATED_KEYS
		);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Edit an existing {@link CachedMessage} object.
	 *
	 * @param message The new Message object.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean update(CachedMessage message) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE message_cache SET message_content = ? WHERE message_id = ?",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setString(1, message.getMessageContent());
		stmt.setLong(2, message.getMessageId());
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Deletes a single {@link CachedMessage} object from the Message Cache.
	 *
	 * @param messageId The message's id.
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean delete(long messageId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM message_cache WHERE message_id = ?",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, messageId);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Delete all messages saved in the Database.
	 *
	 * @return Whether there were rows affected by this process.
	 * @throws SQLException If an error occurs.
	 */
	public boolean deleteAll() throws SQLException {
		PreparedStatement stmt = con.prepareStatement("TRUNCATE TABLE message_cache",
				Statement.RETURN_GENERATED_KEYS
		);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Gets all Messages from the Database.
	 * @return A {@link List} of {@link CachedMessage}s.
	 * @throws SQLException If anything goes wrong.
	 */
	public List<CachedMessage> getAll() throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM message_cache");
		var rs = s.executeQuery();
		List<CachedMessage> cachedMessages = new ArrayList<>();
		while (rs.next()) {
			cachedMessages.add(this.read(rs));
		}
		return cachedMessages;
	}

	/**
	 * Deletes the given amount of Messages.
	 *
	 * @param amount The amount to delete.
	 * @return If any rows we're affected.
	 * @throws SQLException If anything goes wrong.
	 */
	public boolean delete(int amount) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM message_cache LIMIT ?",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setInt(1, amount);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Returns the last cached Message of the Message Cache.
	 *
	 * @return The last {@link CachedMessage}.
	 * @throws SQLException If an error occurs.
	 */
	public CachedMessage getLast() throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM message_cache ORDER BY message_id LIMIT 1");
		var rs = s.executeQuery();
		CachedMessage message = null;
		while (rs.next()) {
			message = this.read(rs);
		}
		return message;
	}

	private CachedMessage read(ResultSet rs) throws SQLException {
		CachedMessage cachedMessage = new CachedMessage();
		cachedMessage.setMessageId(rs.getLong("message_id"));
		cachedMessage.setAuthorId(rs.getLong("author_id"));
		cachedMessage.setMessageContent(rs.getString("message_content"));
		return cachedMessage;
	}
}
