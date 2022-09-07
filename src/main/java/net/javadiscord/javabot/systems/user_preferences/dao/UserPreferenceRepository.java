package net.javadiscord.javabot.systems.user_preferences.dao;

import net.javadiscord.javabot.data.h2db.DatabaseRepository;
import net.javadiscord.javabot.data.h2db.DbActions;
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
	 * @param dbActions A service object responsible for various operations on the main database
	 * @param con The {@link Connection} to use.
	 */
	public UserPreferenceRepository(DbActions dbActions, Connection con) {
		super(con, UserPreference.class, "USER_PREFERENCES", List.of(
				TableProperty.of("user_id", H2Type.BIGINT, (x, y) -> x.setUserId((Long) y), UserPreference::getUserId),
				TableProperty.of("ordinal", H2Type.INTEGER, (x, y) -> x.setPreference(Preference.values()[(Integer) y]), p -> p.getPreference().ordinal()),
				TableProperty.of("enabled", H2Type.BOOLEAN, (x, y) -> x.setEnabled((Boolean) y), UserPreference::isEnabled)
		), dbActions);
	}

	public Optional<UserPreference> getById(long userId, @NotNull Preference preference) throws SQLException {
		return querySingle("WHERE user_id = ? AND ordinal = ?", userId, preference.ordinal());
	}

	public boolean updateState(long userId, @NotNull Preference preference, boolean enabled) throws SQLException {
		return update("UPDATE user_preferences SET enabled = ? WHERE user_id = ? AND ordinal = ?", enabled, userId, preference.ordinal()) > 0;
	}
}
