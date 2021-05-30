package com.javadiscord.javabot.other;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Contains some one-off utility methods for dealing with dates and times.
 */
public class TimeUtils {
	/**
	 * The standard formatter for date time objects.
	 */
	public static final DateTimeFormatter STANDARD_FORMATTER;
	static {
		STANDARD_FORMATTER = DateTimeFormatter.ofPattern("EEE',' dd/MM/yyyy',' HH:mm", Locale.ENGLISH);
	}

	/**
	 * Returns the duration between the given time and now, respecting time zone
	 * information.
	 * @param time The time to get the duration to.
	 * @return A duration between the given time and now.
	 */
	public static Duration durationToNow(OffsetDateTime time) {
		return Duration.between(time, OffsetDateTime.now(time.getOffset()));
	}

	/**
	 * Formats a duration as roughly-accurate string that is human-friendly.
	 * @param duration The duration to format.
	 * @return A string representing a human-readable duration.
	 */
	public static String formatDuration(Duration duration) {
		if (duration.toDays() > 365) {
			return String.format("%d years, %d days", duration.toDays() / 365, duration.toDays() % 365);
		} else if (duration.toDays() > 2) {
			return String.format("%d days", duration.toDays());
		} else if (duration.toHours() > 2) {
			return String.format("%d hours", duration.toHours());
		} else if (duration.toMinutes() > 2){
			return String.format("%d minutes", duration.toMinutes());
		} else if (duration.toSeconds() > 2) {
			return String.format("%d seconds", duration.toSeconds());
		} else {
			return String.format("%d milliseconds", duration.toMillis());
		}
	}

	/**
	 * Shortcut to get a formatted duration string representing the duration
	 * from the given time to now.
	 * @param time The time to use.
	 * @return A string representing the duration.
	 */
	public static String formatDurationToNow(OffsetDateTime time) {
		return formatDuration(durationToNow(time));
	}
}
