package net.javadiscord.javabot.systems.user_preferences;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.user_preferences.dao.UserPreferenceRepository;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;
import net.javadiscord.javabot.util.ExceptionLogger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles & manages user preferences.
 */
@RequiredArgsConstructor
public class UserPreferenceManager {
	private final DataSource dataSource;

	/**
	 * Simply sets the state of the specified {@link Preference} for the specified user.
	 * If no entry for that user is found, this will simply create a new one.
	 *
	 * @param userId     The users' id.
	 * @param preference The {@link Preference} to change the state for.
	 * @param enabled    The preferences' state.
	 * @return Whether the operation was successful.
	 */
	public boolean set(long userId, Preference preference, boolean enabled) {
		try (Connection con = dataSource.getConnection()) {
			UserPreferenceRepository repo = new UserPreferenceRepository(con);
			Optional<UserPreference> preferenceOptional = repo.getById(userId, preference);
			if (preferenceOptional.isPresent()) {
				return repo.updateState(userId, preference, enabled);
			} else {
				UserPreference userPreference = new UserPreference();
				userPreference.setUserId(userId);
				userPreference.setOrdinal(preference.ordinal());
				userPreference.setEnabled(enabled);
				repo.insert(userPreference, false);
				return true;
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return false;
		}
	}
}
