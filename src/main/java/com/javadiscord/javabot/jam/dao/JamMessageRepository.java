package com.javadiscord.javabot.jam.dao;

import com.javadiscord.javabot.jam.model.Jam;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class JamMessageRepository {
	private final Connection con;

	public void saveMessageId(Jam jam, long messageId, String messageType) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO jam_message_id (jam_id, message_id, message_type) VALUES (?, ?, ?)");
		stmt.setLong(1, jam.getId());
		stmt.setLong(2, messageId);
		stmt.setString(3, messageType);
		stmt.executeUpdate();
		stmt.close();
	}

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

	public void removeMessageId(Jam jam, String messageType) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_message_id WHERE jam_id = ? AND message_type = ?");
		stmt.setLong(1, jam.getId());
		stmt.setString(2, messageType);
		stmt.executeUpdate();
		stmt.close();
	}
}
