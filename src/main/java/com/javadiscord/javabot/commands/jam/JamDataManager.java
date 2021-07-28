package com.javadiscord.javabot.commands.jam;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import com.javadiscord.javabot.data.DatabaseHelper;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JamDataManager {

	public void saveJam(Jam jam) throws SQLException, IOException {
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/insert_jam.sql"), Statement.RETURN_GENERATED_KEYS);
		stmt.setLong(1, jam.getGuildId());
		stmt.setLong(2, jam.getStartedBy());
		stmt.setDate(3, Date.valueOf(jam.getStartsAt()));
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("New Jam was not inserted.");
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			jam.setId(rs.getLong(1));
		}
		stmt.close();
		con.close();
	}

	public Jam getJam(long id) {
		try {
			Connection con = Bot.dataSource.getConnection();
			con.setReadOnly(true);
			PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/select_jam_by_id.sql"));
			stmt.setLong(1, id);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			if (rs.next()) {
				return this.readJam(rs);
			} else {
				return null;
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Jam getActiveJam(long guildId) {
		try {
			Connection con = Bot.dataSource.getConnection();
			con.setReadOnly(true);
			PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/find_active_jams.sql"));
			stmt.setLong(1, guildId);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			if (rs.next()) {
				return this.readJam(rs);
			} else {
				return null;
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Jam readJam(ResultSet rs) throws SQLException {
		Jam jam = new Jam();
		jam.setId(rs.getLong("id"));
		jam.setGuildId(rs.getLong("guild_id"));
		jam.setStartedBy(rs.getLong("started_by"));
		jam.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		jam.setStartsAt(rs.getDate("starts_at").toLocalDate());
		jam.setCompleted(rs.getBoolean("completed"));
		jam.setCurrentPhase(rs.getString("current_phase"));
		return jam;
	}

	public void completeJam(Jam jam) throws IOException, SQLException {
		jam.setCompleted(true);
		jam.setCurrentPhase(null);
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/complete_jam.sql"));
		stmt.setLong(1, jam.getId());
		stmt.executeUpdate();
		stmt.close();
		con.close();
	}

	public void updateJamPhase(Jam jam, String nextPhaseName) throws IOException, SQLException {
		jam.setCurrentPhase(nextPhaseName);
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/update_jam_phase.sql"));
		stmt.setString(1, nextPhaseName);
		stmt.setLong(2, jam.getId());
		stmt.executeUpdate();
		stmt.close();
		con.close();
	}

	public void addTheme(Jam jam, JamTheme theme) throws SQLException, IOException {
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/add_theme.sql"), Statement.RETURN_GENERATED_KEYS);
		stmt.setLong(1, jam.getId());
		stmt.setString(2, theme.getName());
		stmt.setString(3, theme.getDescription());
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("New theme was not inserted.");
		stmt.close();
		con.close();
	}

	public List<JamTheme> getThemes(Jam jam) throws SQLException, IOException {
		Connection con = Bot.dataSource.getConnection();
		con.setReadOnly(true);
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/get_jam_themes.sql"));
		stmt.setLong(1, jam.getId());
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		List<JamTheme> themes = new ArrayList<>();
		while (rs.next()) {
			JamTheme theme = new JamTheme();
			theme.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
			theme.setJam(jam);
			theme.setName(rs.getString("name"));
			theme.setDescription(rs.getString("description"));
			theme.setAccepted(rs.getBoolean("accepted"));
			if (rs.wasNull()) {
				theme.setAccepted(null);
			}
			themes.add(theme);
		}
		stmt.close();
		con.close();
		return themes;
	}

	public void removeTheme(JamTheme theme) throws SQLException {
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_theme WHERE jam_id = ? AND name = ?");
		stmt.setLong(1, theme.getJam().getId());
		stmt.setString(2, theme.getName());
		stmt.executeUpdate();
		stmt.close();
		con.close();
	}

	public void saveMessageId(Jam jam, long messageId, String messageType) throws SQLException {
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement("INSERT INTO jam_message_id (jam_id, message_id, message_type) VALUES (?, ?, ?)");
		stmt.setLong(1, jam.getId());
		stmt.setLong(2, messageId);
		stmt.setString(3, messageType);
		stmt.executeUpdate();
		stmt.close();
		con.close();
	}

	public Long getMessageId(Jam jam, String messageType) throws SQLException {
		Connection con = Bot.dataSource.getConnection();
		PreparedStatement stmt = con.prepareStatement("SELECT message_id FROM jam_message_id WHERE jam_id = ? AND message_type = ?");
		stmt.setLong(1, jam.getId());
		stmt.setString(2, messageType);
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		Long messageId = rs.next() ? rs.getLong(1) : null;
		stmt.close();
		con.close();
		return messageId;
	}
}
