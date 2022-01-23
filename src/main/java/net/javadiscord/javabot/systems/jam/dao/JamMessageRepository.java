package net.javadiscord.javabot.systems.jam.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.jam.model.Jam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Dao class that represents the JAM_MESSAGE_ID SQL Table.
 */
@RequiredArgsConstructor
public class JamMessageRepository {
	private final Connection con;

	/**
	 * Inserts a new message id into the database table.
	 *
	 * @param jam         The current {@link Jam}.
	 * @param messageId   The message's id.
	 * @param messageType The message's type.
	 * @throws SQLException If an error occurs.
	 */
	public void saveMessageId(Jam jam, long messageId, String messageType) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO jam_message_id (jam_id, message_id, message_type) VALUES (?, ?, ?)");
		stmt.setLong(1, jam.getId());
		stmt.setLong(2, messageId);
		stmt.setString(3, messageType);
		stmt.executeUpdate();
		stmt.close();
	}

	/**
	 * Gets a message based on the current {@link Jam} and the message's type.
	 *
	 * @param jam         The current {@link Jam}.
	 * @param messageType The message's type.
	 * @return The message's id as a Long.
	 * @throws SQLException If an error occurs.
	 */
	public Long getMessageId(Jam jam, String messageType) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT message_id FROM jam_message_id WHERE jam_id = ? AND message_type = ?");
		stmt.setLong(1, jam.getId());
		stmt.setString(2, messageType);
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		Long messageId = rs.next() ? rs.getLong(1) : null;
		stmt.close();
		return messageId;
	}

	/**
	 * Removes a message based on the current {@link Jam} and the message's type.
	 *
	 * @param jam         The current {@link Jam}.
	 * @param messageType The message's type.
	 * @throws SQLException If an error occurs.
	 */
	public void removeMessageId(Jam jam, String messageType) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_message_id WHERE jam_id = ? AND message_type = ?");
		stmt.setLong(1, jam.getId());
		stmt.setString(2, messageType);
		stmt.executeUpdate();
		stmt.close();
	}
}
