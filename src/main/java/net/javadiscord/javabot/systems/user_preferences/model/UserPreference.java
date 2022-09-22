package net.javadiscord.javabot.systems.user_preferences.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class which represents a single user preference.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
	private long userId;
	private Preference preference;
	private boolean enabled;
}
