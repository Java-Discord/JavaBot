package net.javadiscord.javabot.systems.user_preferences.dao;

import net.javadiscord.javabot.data.h2db.DatabaseRepository;
import net.javadiscord.javabot.data.h2db.TableProperty;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;
import org.h2.api.H2Type;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the USER_PREFERENCES SQL Table.
 */
public class UserPreferenceRepository extends DatabaseRepository<UserPreference> {
	/**
	 * The constructor of this {@link DatabaseRepository} class which defines all important information
	 * about the USER_PREFERENCES database table.
	 *
	 * @param con The {@link Connection} to use.
	 */
	public UserPreferenceRepository(Connection con) {
		super(con, UserPreference.class, "USER_PREFERENCES", List.of(
				TableProperty.of("user_id", H2Type.BIGINT, (x, y) -> x.setUserId((Long) y), UserPreference::getUserId),
				TableProperty.of("ordinal", H2Type.INTEGER, (x, y) -> x.setPreference(Preference.values()[(Integer) y]), p -> p.getPreference().ordinal()),
				TableProperty.of("state", H2Type.VARCHAR, (x, y) -> x.setState((String) y), UserPreference::getState)
		));
	}

	public Optional<UserPreference> getById(long userId, @NotNull Preference preference) throws SQLException {
		return querySingle("WHERE user_id = ? AND ordinal = ?", userId, preference.ordinal());
	}

	public boolean updateState(long userId, @NotNull Preference preference, String state) throws SQLException {
		return update("UPDATE user_preferences SET state = ? WHERE user_id = ? AND ordinal = ?", state, userId, preference.ordinal()) > 0;
	}
}
