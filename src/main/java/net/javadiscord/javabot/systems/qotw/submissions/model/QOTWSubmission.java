package net.javadiscord.javabot.systems.qotw.submissions.model;

import lombok.Data;

/**
 * Simple data class that represents a single QOTW-Submission.
 */
@Data
public class QOTWSubmission {
	private long threadId;
	private int questionNumber;
	private long guildId;
	private long authorId;
	private boolean reviewed;
	private boolean accepted;
}
