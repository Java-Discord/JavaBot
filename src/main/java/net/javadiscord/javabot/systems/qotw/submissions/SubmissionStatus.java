package net.javadiscord.javabot.systems.qotw.submissions;

/**
 * Represents a submissions' status.
 */
public enum SubmissionStatus {
	/**
	 * The submission got accepted and was among the best answers for the current week.
	 */
	ACCEPT_BEST,
	/**
	 * The submission simply got accepted.
	 */
	ACCEPT,
	/**
	 * The submission got declined.
	 */
	DECLINE
}
