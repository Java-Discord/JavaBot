package net.javadiscord.javabot.systems.user_preferences.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;

/**
 * Dao class that represents the USER_PREFERENCES SQL Table.
 */
@Repository
@RequiredArgsConstructor
public class UserPreferenceRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Gets a specific preference value of a user.
	 * @param userId the ID of the user
	 * @param preference the preference to obtain the value from
	 * @return An {@link Optional} containing the value of the preference
	 * @throws DataAccessException if any error occured
	 */
	public Optional<UserPreference> getById(long userId, @NotNull Preference preference) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM USER_PREFERENCES WHERE user_id = ? AND ordinal = ?", (rs, row)->this.read(rs),
					userId, preference.ordinal()));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	public boolean updateState(long userId, @NotNull Preference preference, String state) throws DataAccessException {
		return jdbcTemplate.update("UPDATE USER_PREFERENCES SET state = ? WHERE user_id = ? AND ordinal = ?",
				state, userId, preference.ordinal()) > 0;
	}

	public void insert(UserPreference instance) throws DataAccessException {
		jdbcTemplate.update("INSERT INTO USER_PREFERENCES (user_id, ordinal, state) VALUES (?,?,?)",
				instance.getUserId(), instance.getPreference().ordinal(), instance.getState());
	}

	private UserPreference read(ResultSet rs) throws SQLException {
		return new UserPreference(rs.getLong("user_id"),Preference.values()[rs.getInt("ordinal")],rs.getString("state"));
	}
}
