package net.javadiscord.javabot.systems.jam.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.jam.model.Jam;

import java.sql.*;

/**
 * Dao class that represents the JAM SQL Table.
 */
@RequiredArgsConstructor
public class JamRepository {
	private final Connection con;

	/**
	 * Insertes a new {@link Jam}.
	 *
	 * @param jam The {@link Jam} object.
	 * @throws SQLException If an error occurs.
	 */
	public void saveNewJam(Jam jam) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO jam (guild_id, name, started_by, starts_at, ends_at) VALUES (?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, jam.getGuildId());
		if (jam.getName() != null) {
			stmt.setString(2, jam.getName());
		} else {
			stmt.setNull(2, Types.VARCHAR);
		}
		stmt.setLong(3, jam.getStartedBy());
		stmt.setDate(4, Date.valueOf(jam.getStartsAt()));
		if (jam.getEndsAt() == null) {
			stmt.setNull(5, Types.DATE);
		} else {
			stmt.setDate(5, Date.valueOf(jam.getEndsAt()));
		}

		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("New Jam was not inserted.");
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			jam.setId(rs.getLong(1));
		}
		stmt.close();
	}

	/**
	 * Updates a single {@link Jam}.
	 *
	 * @param jam The updated {@link Jam} object.
	 * @throws SQLException If an error occurs.
	 */
	public void updateJam(Jam jam) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE jam SET name = ?, starts_at = ?, ends_at = ? WHERE id = ?");
		stmt.setString(1, jam.getName());
		stmt.setDate(2, Date.valueOf(jam.getStartsAt()));
		if (jam.getEndsAt() == null) {
			stmt.setNull(3, Types.DATE);
		} else {
			stmt.setDate(3, Date.valueOf(jam.getEndsAt()));
		}
		stmt.setLong(4, jam.getId());
		stmt.executeUpdate();
		stmt.close();
	}

	/**
	 * Gets a single {@link Jam} based on the given id.
	 *
	 * @param id The jam's id.
	 * @return The {@link Jam} object.
	 */
	public Jam getJam(long id) {
		try {
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM jam WHERE id = ?");
			stmt.setLong(1, id);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			if (rs.next()) {
				return this.readJam(rs);
			} else {
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets the active {@link Jam}.
	 *
	 * @param guildId The current guild's id.
	 * @return The {@link Jam} object.
	 */
	public Jam getActiveJam(long guildId) {
		try {
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM jam WHERE guild_id = ? AND completed = FALSE");
			stmt.setLong(1, guildId);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			if (rs.next()) {
				return this.readJam(rs);
			} else {
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Jam readJam(ResultSet rs) throws SQLException {
		Jam jam = new Jam();
		jam.setId(rs.getLong("id"));
		jam.setName(rs.getString("name"));
		jam.setGuildId(rs.getLong("guild_id"));
		jam.setStartedBy(rs.getLong("started_by"));
		jam.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		jam.setStartsAt(rs.getDate("starts_at").toLocalDate());
		var rawDate = rs.getDate("ends_at");
		jam.setEndsAt(rawDate == null ? null : rawDate.toLocalDate());
		jam.setCompleted(rs.getBoolean("completed"));
		jam.setCurrentPhase(rs.getString("current_phase"));
		return jam;
	}

	/**
	 * Completes a single {@link Jam}.
	 *
	 * @param jam The {@link Jam} object to update.
	 * @throws SQLException If an error occurs.
	 */
	public void completeJam(Jam jam) throws SQLException {
		jam.setCompleted(true);
		jam.setCurrentPhase(null);
		PreparedStatement stmt = con.prepareStatement("UPDATE jam SET completed = TRUE, current_phase = NULL WHERE id = ?");
		stmt.setLong(1, jam.getId());
		stmt.executeUpdate();
		stmt.close();
	}

	/**
	 * Updates a single {@link Jam}'s phase.
	 *
	 * @param jam           The {@link Jam} object to update.
	 * @param nextPhaseName The next phase's name.
	 * @throws SQLException If an error occurs.
	 */
	public void updateJamPhase(Jam jam, String nextPhaseName) throws SQLException {
		jam.setCurrentPhase(nextPhaseName);
		PreparedStatement stmt = con.prepareStatement("UPDATE jam SET current_phase = ? WHERE id = ?");
		stmt.setString(1, nextPhaseName);
		stmt.setLong(2, jam.getId());
		stmt.executeUpdate();
		stmt.close();
	}

	/**
	 * Cancels a single {@link Jam}.
	 *
	 * @param jam The {@link Jam} object to cancel.
	 * @throws SQLException If an error occurs.
	 */
	public void cancelJam(Jam jam) throws SQLException {
		this.completeJam(jam);
		PreparedStatement stmt = con.prepareStatement("DELETE FROM jam_message_id WHERE jam_id = ?");
		stmt.setLong(1, jam.getId());
		stmt.executeUpdate();
		stmt.close();
		stmt = con.prepareStatement("UPDATE jam_theme SET accepted = FALSE WHERE jam_id = ? AND accepted IS NULL;");
		stmt.setLong(1, jam.getId());
		stmt.executeUpdate();
		stmt.close();
	}
}
