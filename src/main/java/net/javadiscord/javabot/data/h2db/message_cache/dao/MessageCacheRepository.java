package net.javadiscord.javabot.data.h2db.message_cache.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Dao class that represents the QOTW_POINTS SQL Table.
 */
@RequiredArgsConstructor
@Repository
public class MessageCacheRepository {
	private final JdbcTemplate jdbcTemplate;

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
		List<Map.Entry<CachedMessage, Integer>> attachments=new ArrayList<>();
		for (CachedMessage msg : messages) {
			for (int i = 0; i < msg.getAttachments().size(); i++) {
				attachments.add(Map.entry(msg, i));
			}
		}
		jdbcTemplate.batchUpdate("MERGE INTO message_cache_attachments (message_id, attachment_index, link) VALUES (?, ?, ?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement stmt, int i) throws SQLException {
						Entry<CachedMessage, Integer> entry = attachments.get(i);
						CachedMessage msg = entry.getKey();
						Integer attachmentIndex = entry.getValue();
						stmt.setLong(1, msg.getMessageId());
						stmt.setInt(2, attachmentIndex);
						stmt.setString(3, msg.getAttachments().get(attachmentIndex));
					}

					@Override
					public int getBatchSize() {
						return attachments.size();
					}
				});
	}

	/**
	 * Gets all Messages from the Database.
	 *
	 * @return A {@link List} of {@link CachedMessage}s.
	 * @throws SQLException If anything goes wrong.
	 */
	public List<CachedMessage> getAll() throws DataAccessException {
		List<CachedMessage> messagesWithLink = jdbcTemplate.query(
				"SELECT * FROM message_cache LEFT JOIN message_cache_attachments ON message_cache.message_id = message_cache_attachments.message_id",
				(rs, rowNum) -> this.read(rs));
		Map<Long, CachedMessage> messages=new LinkedHashMap<>();
		for (CachedMessage msg : messagesWithLink) {
			CachedMessage previous = messages.putIfAbsent(msg.getMessageId(), msg);
			if(previous!=null) {
				previous.getAttachments().addAll(msg.getAttachments());
			}
		}
		return new ArrayList<>(messages.values());
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
		if(rows > 0){
			jdbcTemplate.update("DELETE FROM message_cache_attachments WHERE message_id NOT IN (SELECT message_id FROM message_cache)");
			return true;
		}
		return false;
	}

	private CachedMessage read(ResultSet rs) throws SQLException {
		CachedMessage cachedMessage = new CachedMessage();
		cachedMessage.setMessageId(rs.getLong("message_cache.message_id"));
		cachedMessage.setAuthorId(rs.getLong("author_id"));
		cachedMessage.setMessageContent(rs.getString("message_content"));
		String attachment = rs.getString("link");
		if(attachment!=null) {
			cachedMessage.getAttachments().add(attachment);
		}
		return cachedMessage;
	}
}
