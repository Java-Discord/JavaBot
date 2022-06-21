package net.javadiscord.javabot.util;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class which unifies the way exceptions are logged.
 */
@Slf4j
public final class ExceptionLogger {
	private ExceptionLogger() {
	}

	/**
	 * Captures a single {@link Throwable}.
	 *
	 * @param t    The {@link Throwable} to log.
	 * @param name The origin's name.
	 */
	public static void capture(Throwable t, String name) {
		Sentry.captureException(t);
		log.error("I've encountered an " + t.getClass().getSimpleName() + " in " + name + ":", t);
	}

	/**
	 * Captures a single {@link Throwable}.
	 *
	 * @param t The {@link Throwable} to log.
	 */
	public static void capture(Throwable t) {
		Sentry.captureException(t);
		log.error("I've encountered an " + t.getClass().getSimpleName() + ": ", t);
	}
}
