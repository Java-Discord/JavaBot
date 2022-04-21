package net.javadiscord.javabot.systems.qotw.submissions.model;

import lombok.Data;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;

/**
 * Simple data class that represents a single QOTW-Submission.
 */
@Data
public class QOTWSubmission {
	private long threadId;
	private int questionNumber;
	private long guildId;
	private long authorId;
	private SubmissionStatus status;
}
