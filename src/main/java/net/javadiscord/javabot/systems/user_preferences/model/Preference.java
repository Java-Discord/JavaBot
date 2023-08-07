package net.javadiscord.javabot.systems.user_preferences.model;

import lombok.Getter;

/**
 * Contains all preferences users can set.
 */
public enum Preference {
    /**
     * Enables/Disables QOTW reminders.
     */
    QOTW_REMINDER("Question of the Week Reminder", "false", new BooleanPreference()),
    /**
     * Enables/Disables DM notifications for dormant help posts.
     */
    PRIVATE_DORMANT_NOTIFICATIONS("Send notifications about dormant help post via DM", "true", new BooleanPreference()),

    /**
     * Enables/Disables DM notifications for closed help posts.
     */
    PRIVATE_CLOSE_NOTIFICATIONS("Send notifications about help posts closed by other users via DM", "true", new BooleanPreference()),
    /**
     * Enables / Disables AutoCodeFormatter for help posts.
     * Used by {@link net.javadiscord.javabot.systems.help.AutoCodeFormatter}
     */
    FORMAT_UNFORMATTED_CODE("Automatically detect and add missing code syntax highlighting", "true", new BooleanPreference());
    private final String name;
    @Getter
	private final String defaultState;
    @Getter
	private final PreferenceType type;

    Preference(String name, String defaultState, PreferenceType type) {
        this.name = name;
        this.defaultState = defaultState;
        this.type = type;
    }

    @Override
    public String toString() {
        return name;
    }

}
