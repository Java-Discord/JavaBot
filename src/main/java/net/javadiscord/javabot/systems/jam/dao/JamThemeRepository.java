package net.javadiscord.javabot.systems.jam.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamTheme;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class JamThemeRepository {
	private final Connection con;
	public void addTheme(Jam jam, JamTheme theme) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO jam_theme (jam_id, name, description) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, jam.getId());
		stmt.setString(2, theme.getName());
		stmt.setString(3, theme.getDescription());
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("New theme was not inserted.");
		stmt.close();
	}

	public List<JamTheme> getThemes(Jam jam) throws SQLException {
		return this.fetchThemes(jam, "SELECT * FROM jam_theme WHERE jam_id = ? ORDER BY name");
	}

	public List<JamTheme> getAcceptedThemes(Jam jam) throws SQLException {
		return this.fetchThemes(jam, "SELECT * FROM jam_theme WHERE accepted = TRUE AND jam_id = ? ORDER BY name");
	}

	private List<JamTheme> fetchThemes(Jam jam, String sql) throws SQLException {
		con.setReadOnly(true);
		PreparedStatement stmt = con.prepareStatement(sql);
		stmt.setLong(1, jam.getId());
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		List<JamTheme> themes = new ArrayList<>();
		while (rs.next()) {
			themes.add(this.readTheme(rs, jam));
		}
		stmt.close();
		return themes;
	}

	private JamTheme readTheme(ResultSet rs, Jam jam) throws SQLException {
		JamTheme theme = new JamTheme();
		theme.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		theme.setJam(jam);
		theme.setName(rs.getString("name"));
		theme.setDescription(rs.getString("description"));
		theme.setAccepted(rs.getBoolean("accepted"));
		if (rs.wasNull()) {
			theme.setAccepted(null);
		}
		return theme;
	}

	public void removeTheme(JamTheme theme) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_theme WHERE jam_id = ? AND name = ?");
		stmt.setLong(1, theme.getJam().getId());
		stmt.setString(2, theme.getName());
		stmt.executeUpdate();
		stmt.close();
	}
}
