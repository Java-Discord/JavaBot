package net.javadiscord.javabot.systems.tags.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.tags.model.CustomTag;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the CUSTOM_COMMANDS SQL Table.
 */
@RequiredArgsConstructor
public class CustomTagRepository {
	private final Connection con;

	/**
	 * Inserts a new warn into the database.
	 *
	 * @param command The custom commands to save.
	 * @return The custom command that was saved.
	 * @throws SQLException If an error occurs.
	 */
	public CustomTag insert(CustomTag command) throws SQLException, IllegalArgumentException {
		if (findByName(command.getGuildId(), command.getName()).isPresent()) {
			throw new IllegalArgumentException(String.format("A Custom Command in Guild %s called %s already exists.", command.getGuildId(), command.getName()));
		}
		try (var s = con.prepareStatement(
				"INSERT INTO custom_tags (guild_id, created_by, name, response, reply, embed) VALUES (?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		)) {
			s.setLong(1, command.getGuildId());
			s.setLong(2, command.getCreatedBy());
			s.setString(3, command.getName());
			s.setString(4, command.getResponse());
			s.setBoolean(5, command.isReply());
			s.setBoolean(6, command.isEmbed());
			s.executeUpdate();
			var rs = s.getGeneratedKeys();
			if (!rs.next()) throw new SQLException("No generated keys returned.");
			long id = rs.getLong(1);
			return findById(id).orElseThrow();
		}
	}

	/**
	 * Edits a Custom Command.
	 *
	 * @param old    The old custom command.
	 * @param update The new custom command.
	 * @return The updated {@link CustomTag} object.
	 * @throws SQLException If an error occurs.
	 */
	public CustomTag edit(@NotNull CustomTag old, CustomTag update) throws SQLException {
		if (findByName(old.getGuildId(), old.getName()).isEmpty()) {
			throw new IllegalArgumentException(String.format("A Custom Command in Guild %s called %s does not exist.", old.getGuildId(), old.getName()));
		}
		try (var s = con.prepareStatement(
				"UPDATE custom_tags SET response = ?, reply = ?, embed = ? WHERE id = ?",
				Statement.RETURN_GENERATED_KEYS
		)) {
			s.setString(1, update.getResponse());
			s.setBoolean(2, update.isReply());
			s.setBoolean(3, update.isEmbed());
			s.setLong(4, old.getId());
			s.executeUpdate();
			var rs = s.getGeneratedKeys();
			if (!rs.next()) throw new SQLException("No generated keys returned.");
			long id = rs.getLong(1);
			return findById(id).orElseThrow();
		}
	}

	/**
	 * Deletes a Custom Command.
	 *
	 * @param command The custom command to delete.
	 * @throws SQLException If an error occurs.
	 */
	public void delete(@NotNull CustomTag command) throws SQLException {
		try (var s = con.prepareStatement("DELETE FROM custom_tags WHERE id = ?")) {
			s.setLong(1, command.getId());
			s.executeUpdate();
		}
	}

	/**
	 * Finds a custom command by its name.
	 *
	 * @param guildId The guild id of the custom command.
	 * @param name    The name of the custom command.
	 * @return The custom command, if it was found.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<CustomTag> findByName(long guildId, String name) throws SQLException {
		CustomTag command = null;
		try (var s = con.prepareStatement("SELECT * FROM custom_tags WHERE guild_id = ? AND name = ?")) {
			s.setLong(1, guildId);
			s.setString(2, name);
			var rs = s.executeQuery();
			if (rs.next()) {
				command = read(rs);
			}
			rs.close();
		}
		return Optional.ofNullable(command);
	}

	/**
	 * Finds a custom command by its id.
	 *
	 * @param id The id of the custom command.
	 * @return The custom command, if it was found.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<CustomTag> findById(long id) throws SQLException {
		CustomTag command = null;
		try (var s = con.prepareStatement("SELECT * FROM custom_tags WHERE id = ?")) {
			s.setLong(1, id);
			var rs = s.executeQuery();
			if (rs.next()) {
				command = read(rs);
			}
			rs.close();
		}
		return Optional.ofNullable(command);
	}

	/**
	 * Gets all custom commands for the given guild.
	 *
	 * @param guildId The id of the guild.
	 * @return A List with all custom commands.
	 */
	public List<CustomTag> getCustomTagsByGuildId(long guildId) {
		List<CustomTag> commands = new ArrayList<>();
		try (var s = con.prepareStatement("SELECT * FROM custom_tags WHERE guild_id = ?")) {
			s.setLong(1, guildId);
			var rs = s.executeQuery();
			while (rs.next()) commands.add(read(rs));
			rs.close();
			return commands;
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
	}

	/**
	 * Reads the given {@link ResultSet} and constructs a new {@link CustomTag} object.
	 *
	 * @param rs The ResultSet.
	 * @return The {@link CustomTag} object.
	 * @throws SQLException If an error occurs.
	 */
	private @NotNull CustomTag read(@NotNull ResultSet rs) throws SQLException {
		CustomTag tag = new CustomTag();
		tag.setId(rs.getLong("id"));
		tag.setGuildId(rs.getLong("guild_id"));
		tag.setCreatedBy(rs.getLong("created_by"));
		tag.setName(rs.getString("name"));
		tag.setResponse(rs.getString("response"));
		tag.setReply(rs.getBoolean("reply"));
		tag.setEmbed(rs.getBoolean("embed"));
		return tag;
	}
}
