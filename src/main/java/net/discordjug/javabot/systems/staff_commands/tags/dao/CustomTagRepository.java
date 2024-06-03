package net.discordjug.javabot.systems.staff_commands.tags.dao;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.systems.staff_commands.tags.model.CustomTag;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dao class that represents the CUSTOM_COMMANDS SQL Table.
 */
@RequiredArgsConstructor
@Repository
public class CustomTagRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a new warn into the database.
	 *
	 * @param command The custom commands to save.
	 * @return The custom command that was saved.
	 * @throws SQLException If an error occurs.
	 */
	public CustomTag insert(CustomTag command) throws DataAccessException, IllegalArgumentException {
		if (findByName(command.getGuildId(), command.getName()).isPresent()) {
			throw new IllegalArgumentException(String.format("A Custom Command in Guild %s called %s already exists.", command.getGuildId(), command.getName()));
		}
		Number key = new SimpleJdbcInsert(jdbcTemplate)
			.withTableName("custom_tags")
			.usingColumns("guild_id","created_by","name","response","reply","embed")
			.usingGeneratedKeyColumns("id")
			.executeAndReturnKey(Map.of(
					"guild_id",command.getGuildId(),
					"created_by",command.getCreatedBy(),
					"name",command.getName(),
					"response",command.getResponse(),
					"reply",command.isReply(),
					"embed",command.isEmbed()
					));
		return findById(key.longValue()).orElseThrow();
	}

	/**
	 * Edits a Custom Command.
	 *
	 * @param old    The old custom command.
	 * @param update The new custom command.
	 * @return The updated {@link CustomTag} object.
	 * @throws SQLException If an error occurs.
	 */
	public CustomTag edit(@NotNull CustomTag old, CustomTag update) throws DataAccessException {
		if (findByName(old.getGuildId(), old.getName()).isEmpty()) {
			throw new IllegalArgumentException(String.format("A Custom Command in Guild %s called %s does not exist.", old.getGuildId(), old.getName()));
		}
		jdbcTemplate.update("UPDATE custom_tags SET response = ?, reply = ?, embed = ? WHERE id = ?",
				update.getResponse(),update.isReply(),update.isEmbed(),old.getId());
		return findById(old.getId()).orElseThrow();
	}

	/**
	 * Deletes a Custom Command.
	 *
	 * @param command The custom command to delete.
	 * @throws SQLException If an error occurs.
	 */
	public void delete(@NotNull CustomTag command) throws DataAccessException {
		jdbcTemplate.update("DELETE FROM custom_tags WHERE id = ?",
				command.getId());
	}

	/**
	 * Finds a custom command by its name.
	 *
	 * @param guildId The guild id of the custom command.
	 * @param name    The name of the custom command.
	 * @return The custom command, if it was found.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<CustomTag> findByName(long guildId, String name) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM custom_tags WHERE guild_id = ? AND name = ?",(rs,row)->this.read(rs),
				guildId, name));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Finds a custom command by its id.
	 *
	 * @param id The id of the custom command.
	 * @return The custom command, if it was found.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<CustomTag> findById(long id) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM custom_tags WHERE id = ?",(rs,row)->this.read(rs),
				id));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets all custom commands for the given guild.
	 *
	 * @param guildId The id of the guild.
	 * @return A List with all custom commands.
	 */
	public List<CustomTag> getCustomTagsByGuildId(long guildId) {
		return jdbcTemplate.query("SELECT * FROM custom_tags WHERE guild_id = ? ORDER BY name", (rs, row)->this.read(rs),
				guildId);
	}
	
	/**
	 * Gets all custom commands for the given guild matching a specified query.
	 * A tag matches the query if the name or reply contains the query.
	 *
	 * @param guildId The id of the guild.
	 * @param query The search query.
	 * @return A List with all custom commands.
	 */
	public List<CustomTag> search(long guildId, String query) {
		String enhancedQuery = "%" + query + "%";
		return jdbcTemplate.query("SELECT * FROM custom_tags WHERE guild_id = ? AND (name LIKE ? OR response LIKE ?) ORDER BY name", (rs, row)->this.read(rs),
				guildId, enhancedQuery, enhancedQuery);
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
