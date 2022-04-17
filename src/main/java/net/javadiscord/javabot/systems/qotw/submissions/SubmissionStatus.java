package net.javadiscord.javabot.systems.qotw.submissions;

/**
 * Enum class that represents the status of QOTW Submissions.
 */
public enum SubmissionStatus {
	/**
	 * Used for submissions that were accepted.
	 */
	ACCEPTED,
	/**
	 * Used for submissions that were declined.
	 */
	DECLINED,
	/**
	 * Used for submissions that were deleted.
	 */
	DELETED,
	/**
	 * Used for submissions that were yet unreviewed.
	 */
	UNREVIEWED
}
