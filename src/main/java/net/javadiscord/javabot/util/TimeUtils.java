package net.javadiscord.javabot.util;

import java.time.Clock;
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

	private final Clock clock;

	public TimeUtils() {
		this(Clock.systemDefaultZone());
	}

	public TimeUtils(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Returns the duration between the given time and now, respecting time zone
	 * information.
	 * @param time The time to get the duration to.
	 * @return A duration between the given time and now.
	 */
	public Duration durationToNow(OffsetDateTime time) {
		return Duration.between(time, this.clock.instant().atOffset(time.getOffset()));
	}

	/**
	 * Shortcut to get a formatted duration string representing the duration
	 * from the given time to now.
	 * @param time The time to use.
	 * @return A string representing the duration.
	 */
	public String formatDurationToNow(OffsetDateTime time) {
		return formatDuration(durationToNow(time));
	}

	/**
	 * Formats a duration as roughly-accurate string that is human-friendly.
	 * @param duration The duration to format.
	 * @return A string representing a human-readable duration.
	 */
	public static String formatDuration(Duration duration) {
		if (duration.toDays() >= 365) {
			long years = duration.toDays() / 365;
			long days = duration.toDays() % 365;
			StringBuilder sb = new StringBuilder(pluralize("year", "years", years));
			if (days > 0) sb.append(", ").append(pluralize("day", "days", days));
			return sb.toString();
		} else if (duration.toDays() > 0) {
			return pluralize("day", "days", duration.toDays());
		} else if (duration.toHours() > 0) {
			return pluralize("hour", "hours", duration.toHours());
		} else if (duration.toMinutes() > 0){
			return pluralize("minute", "minutes", duration.toMinutes());
		} else if (duration.toSeconds() > 0) {
			return pluralize("second", "seconds", duration.toSeconds());
		} else {
			return pluralize("millisecond", "milliseconds", duration.toMillis());
		}
	}

	/**
	 * Formats an integer number with a suffix that agrees with the plurality of
	 * the number. For example, this can produce "1 day" and "2 days".
	 * @param single The singular form of the suffix.
	 * @param plural The plural form of the suffix.
	 * @param count The value to show.
	 * @return A string representation that respects plurality.
	 */
	private static String pluralize(String single, String plural, long count) {
		return count + " " + (count == 1 ? single : plural);
	}
}
