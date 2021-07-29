package com.javadiscord.javabot.commands.jam.dao;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.data.DatabaseHelper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.sql.*;

@RequiredArgsConstructor
public class JamRepository {
	private final Connection con;
	public void saveJam(Jam jam) throws SQLException, IOException {
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
	}

	public Jam getJam(long id) {
		try {
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
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/complete_jam.sql"));
		stmt.setLong(1, jam.getId());
		stmt.executeUpdate();
		stmt.close();
	}

	public void updateJamPhase(Jam jam, String nextPhaseName) throws IOException, SQLException {
		jam.setCurrentPhase(nextPhaseName);
		PreparedStatement stmt = con.prepareStatement(DatabaseHelper.loadSql("/jam/sql/update_jam_phase.sql"));
		stmt.setString(1, nextPhaseName);
		stmt.setLong(2, jam.getId());
		stmt.executeUpdate();
		stmt.close();
	}
}
