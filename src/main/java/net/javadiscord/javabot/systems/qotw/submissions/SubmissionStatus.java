package net.javadiscord.javabot.systems.qotw.submissions;

/**
 * Represents a submissions' status.
 */
public enum SubmissionStatus {
	/**
	 * The submission got accepted and was among the best answers for the current week.
	 */
	ACCEPT_BEST("accepted (best answer)"),
	/**
	 * The submission simply got accepted.
	 */
	ACCEPT("accepted"),
	/**
	 * The submission got declined.
	 */
	DECLINE("declined");

	private final String verb;

	SubmissionStatus(String verb) {
		this.verb = verb;
	}

	public String getVerb() {
		return verb;
	}
}
