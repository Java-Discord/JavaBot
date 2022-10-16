package net.javadiscord.javabot.systems.user_preferences;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.user_preferences.dao.UserPreferenceRepository;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;
import net.javadiscord.javabot.util.ExceptionLogger;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles & manages user preferences.
 */
@RequiredArgsConstructor
@Service
public class UserPreferenceService {
	private final DataSource dataSource;
	private final UserPreferenceRepository userPreferenceRepository;

	/**
	 * Simply sets the state of the specified {@link Preference} for the specified user.
	 * If no entry for that user is found, this will simply create a new one.
	 *
	 * @param userId     The users' id.
	 * @param preference The {@link Preference} to change the state for.
	 * @param state    The preferences' state.
	 * @return Whether the operation was successful.
	 */
	public boolean setOrCreate(long userId, Preference preference, String state) {
		try (Connection con = dataSource.getConnection()) {
			Optional<UserPreference> preferenceOptional = userPreferenceRepository.getById(userId, preference);
			if (preferenceOptional.isPresent()) {
				return userPreferenceRepository.updateState(userId, preference, state);
			} else {
				UserPreference userPreference = new UserPreference();
				userPreference.setUserId(userId);
				userPreference.setPreference(preference);
				userPreference.setState(state);
				userPreferenceRepository.insert(userPreference);
				return true;
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return false;
		}
	}

	/**
	 * Gets a single {@link UserPreference} (or creates a new one if it doesn't exist yet).
	 *
	 * @param userId     The users' id.
	 * @param preference The {@link Preference} to get.
	 * @return The {@link UserPreference}.
	 */
	public UserPreference getOrCreate(long userId, Preference preference) {
		try (Connection con = dataSource.getConnection()) {
			Optional<UserPreference> preferenceOptional = userPreferenceRepository.getById(userId, preference);
			if (preferenceOptional.isPresent()) {
				return preferenceOptional.get();
			} else {
				UserPreference userPreference = new UserPreference();
				userPreference.setUserId(userId);
				userPreference.setPreference(preference);
				userPreference.setState(preference.getDefaultState());
				userPreferenceRepository.insert(userPreference);
				return userPreference;
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return null;
		}
	}
}
