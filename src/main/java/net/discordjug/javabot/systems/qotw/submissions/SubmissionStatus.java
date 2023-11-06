package net.discordjug.javabot.systems.qotw.submissions;

/**
 * Represents a submissions' status.
 */
public enum SubmissionStatus {
	/**
	 * The submission got accepted and was among the best answers for the current week.
	 */
	ACCEPT_BEST("accepted (best answer)"),
	/**
	 * The submission got accepted but was not among the best answers for the current week.
	 */
	ACCEPT("accepted"),
	/**
	 * The submission got declined as it was simply wrong.
	 */
	DECLINE_WRONG_ANSWER("declined (wrong answer)"),
	/**
	 * The submission got declined as it was too short compared to other submissions.
	 */
	DECLINE_TOO_SHORT("declined (too short)"),
	/**
	 * The submission got declined as it was simply empty.
	 */
	DECLINE_EMPTY("declined (empty)");

	private final String verb;

	SubmissionStatus(String verb) {
		this.verb = verb;
	}

	public String getVerb() {
		return verb;
	}
}
