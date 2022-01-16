package net.javadiscord.javabot.systems.starboard.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.systems.starboard.model.StarboardEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class StarboardRepository {
	private final Connection con;

	public void insert(StarboardEntry entry) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO starboard (original_message_id, guild_id, channel_id, author_id, starboard_message_id) VALUES (?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, entry.getOriginalMessageId());
		stmt.setLong(2, entry.getGuildId());
		stmt.setLong(3, entry.getChannelId());
		stmt.setLong(4, entry.getAuthorId());
		stmt.setLong(5, entry.getStarboardMessageId());
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("Starboard Entry was not inserted.");
		stmt.close();
		log.info("Inserted new Starboard-Entry: {}", entry);
	}

	public void delete(long messageId) throws SQLException {
		try (var stmt = con.prepareStatement("""
			DELETE FROM starboard
			WHERE original_message_id = ?""")) {
			stmt.setLong(1, messageId);
			stmt.executeUpdate();
		}
	}

	public StarboardEntry getEntryByMessageId(long messageId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM starboard WHERE original_message_id = ?");
		s.setLong(1, messageId);
		var rs = s.executeQuery();
		if (rs.next()) {
			return read(rs);
		}
		return null;
	}

	public StarboardEntry getEntryByStarboardMessageId(long starboardMessageId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM starboard WHERE starboard_message_id = ?");
		s.setLong(1, starboardMessageId);
		var rs = s.executeQuery();
		if (rs.next()) {
			return read(rs);
		}
		return null;
	}

	public List<StarboardEntry> getAllStarboardEntries(long guildId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM starboard WHERE guild_id = ?");
		s.setLong(1, guildId);
		var rs = s.executeQuery();
		List<StarboardEntry> entries = new ArrayList<>();
		while (rs.next()) {
			entries.add(read(rs));
		}
		return entries;
	}

	private StarboardEntry read(ResultSet rs) throws SQLException {
		StarboardEntry entry = new StarboardEntry();
		entry.setOriginalMessageId(rs.getLong("original_message_id"));
		entry.setGuildId(rs.getLong("guild_id"));
		entry.setChannelId(rs.getLong("channel_id"));
		entry.setAuthorId(rs.getLong("author_id"));
		entry.setStarboardMessageId(rs.getLong("starboard_message_id"));
		return entry;
	}
}
