package net.javadiscord.javabot.data.h2db.message_cache.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
public class MessageCacheRepository {
	private final Connection con;

	/**
	 * Converts a {@link Message} object to a {@link CachedMessage}.
	 *
	 * @param message The {@link Message} to convert.
	 * @return The built {@link CachedMessage}.
	 */
	public static CachedMessage toCachedMessage(Message message) {
		CachedMessage cachedMessage = new CachedMessage();
		cachedMessage.setMessageId(message.getIdLong());
		cachedMessage.setAuthorId(message.getAuthor().getIdLong());
		cachedMessage.setMessageContent(message.getContentRaw().trim());
		return cachedMessage;
	}

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
	 * Edit an existing {@link Message} object.
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
	 * Retrieves a single {@link CachedMessage} from the Message Cache.
	 *
	 * @param messageId The message's id.
	 * @return The {@link CachedMessage} object - as an {@link Optional}.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<CachedMessage> getByMessageId(long messageId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM message_cache WHERE message_id = ?");
		s.setLong(1, messageId);
		var rs = s.executeQuery();
		CachedMessage cachedMessage = null;
		if (rs.next()) {
			cachedMessage = this.read(rs);
		}
		return Optional.ofNullable(cachedMessage);
	}

	/**
	 * Gets all cached messages of a single user.
	 *
	 * @param userId The user's id.
	 * @return A list of {@link CachedMessage} objects.
	 * @throws SQLException If an error occurs.
	 */
	public List<CachedMessage> getByUserId(long userId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM message_cache WHERE author_id = ?");
		s.setLong(1, userId);
		var rs = s.executeQuery();
		List<CachedMessage> messages = new ArrayList<>();
		while (rs.next()) {
			messages.add(this.read(rs));
		}
		return messages;
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
