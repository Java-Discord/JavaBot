package net.javadiscord.javabot.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the functionality of the {@link TimeUtils} class.
 */
public class TimeUtilsTest {
	@Test
	public void testDurationToNow() {
		OffsetDateTime now = OffsetDateTime.of(2020, 6, 1, 10, 16, 45, 0, ZoneOffset.UTC);
		TimeUtils tu = new TimeUtils(Clock.fixed(now.toInstant(), ZoneOffset.UTC));
		assertEquals(3, tu.durationToNow(now.minusDays(3)).toDays());
		assertEquals(2, tu.durationToNow(now.minusHours(2).atZoneSameInstant(ZoneId.of("EST", ZoneId.SHORT_IDS)).toOffsetDateTime()).toHours());
	}

	@Test
	public void testFormatDuration() {
		Map<Duration, String> cases = new HashMap<>();
		cases.put(Duration.ofDays(0), "0 milliseconds");
		cases.put(Duration.ofDays(1), "1 day");
		cases.put(Duration.ofDays(2), "2 days");
		cases.put(Duration.ofMillis(2), "2 milliseconds");
		cases.put(Duration.ofMillis(1), "1 millisecond");
		cases.put(Duration.ofSeconds(1), "1 second");
		cases.put(Duration.ofSeconds(3), "3 seconds");
		cases.put(Duration.ofMinutes(1), "1 minute");
		cases.put(Duration.ofMinutes(45), "45 minutes");
		cases.put(Duration.ofHours(1), "1 hour");
		cases.put(Duration.ofHours(2), "2 hours");
		cases.put(Duration.ofDays(366), "1 year, 1 day");
		cases.put(Duration.ofDays(365), "1 year");
		cases.put(Duration.ofDays(730), "2 years");
		cases.put(Duration.ofDays(731), "2 years, 1 day");
		cases.put(Duration.ofDays(732), "2 years, 2 days");

		for (var c : cases.entrySet()) {
			String actual = TimeUtils.formatDuration(c.getKey());
			assertEquals(c.getValue(), actual);
		}
	}
}
