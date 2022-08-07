package net.javadiscord.javabot.systems.qotw.submissions.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the QOTW_SUBMISSIONS SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
public class QOTWSubmissionRepository {
	private final Connection con;

	/**
	 * Inserts a new {@link QOTWSubmission}.
	 *
	 * @param submission The account to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(QOTWSubmission submission) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("INSERT INTO qotw_submissions (thread_id, question_number, guild_id, author_id) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		)) {
			stmt.setLong(1, submission.getThreadId());
			stmt.setInt(2, submission.getQuestionNumber());
			stmt.setLong(3, submission.getGuildId());
			stmt.setLong(4, submission.getAuthorId());
			int rows = stmt.executeUpdate();
			if (rows == 0) throw new SQLException("Submission was not inserted.");
			log.info("Inserted new QOTW-Submission: {}", submission);
		}
	}

	/**
	 * Removes a single {@link QOTWSubmission}.
	 *
	 * @param threadId The current thread's id.
	 * @return Whether the {@link QOTWSubmission} was actually removed.
	 * @throws SQLException If an error occurs.
	 */
	public boolean deleteSubmission(long threadId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("DELETE FROM qotw_submissions WHERE thread_id = ?")) {
			stmt.setLong(1, threadId);
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}

	/**
	 * Updates the status of a single {@link QOTWSubmission}.
	 *
	 * @param threadId The submission's thread id.
	 * @param status The new {@link SubmissionStatus}.
	 * @throws SQLException If an error occurs.
	 */
	public void updateStatus(long threadId, @NotNull SubmissionStatus status) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("UPDATE qotw_submissions SET status = ? WHERE thread_id = ?")) {
			stmt.setInt(1, status.ordinal());
			stmt.setLong(2, threadId);
			stmt.executeUpdate();
		}
	}

	/**
	 * Gets all unreviewed {@link QOTWSubmission}'s from a single user.
	 *
	 * @param authorId The user's id.
	 * @return A List of unreviewed {@link QOTWSubmission}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<QOTWSubmission> getUnreviewedSubmissions(long authorId) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE author_id = ? AND status = 0")) {
			s.setLong(1, authorId);
			ResultSet rs = s.executeQuery();
			List<QOTWSubmission> submissions = new ArrayList<>();
			while (rs.next()) {
				submissions.add(read(rs));
			}
			return submissions;
		}
	}

	/**
	 * Returns a {@link QOTWSubmission} based on the given thread id.
	 *
	 * @param threadId The discord Id of the user.
	 * @return The {@link QOTWSubmission} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWSubmission> getSubmissionByThreadId(long threadId) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE thread_id = ?")) {
			s.setLong(1, threadId);
			ResultSet rs = s.executeQuery();
			QOTWSubmission submission = null;
			if (rs.next()) {
				submission = read(rs);
			}
			return Optional.ofNullable(submission);
		}
	}

	/**
	 * Returns the current Question Number.
	 *
	 * @return The current Question Number.
	 * @throws SQLException If an error occurs.
	 */
	public int getCurrentQuestionNumber() throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT MAX(question_number) FROM qotw_submissions")) {
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		}
	}

	/**
	 * Returns all {@link QOTWSubmission}s based on the given question number.
	 *
	 * @param guildId the ID of the guild
	 * @param questionNumber The week's number.
	 * @return All {@link QOTWSubmission}s, as a {@link List}.
	 * @throws SQLException If an error occurs.
	 */
	public List<QOTWSubmission> getSubmissionsByQuestionNumber(long guildId, int questionNumber) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE guild_id = ? AND question_number = ?")) {
			s.setLong(1, guildId);
			s.setInt(2, questionNumber);
			ResultSet rs = s.executeQuery();
			List<QOTWSubmission> submissions = new ArrayList<>();
			while (rs.next()) {
				submissions.add(this.read(rs));
			}
			return submissions;
		}
	}

	/**
	 * Returns the {@link QOTWSubmission} of a specific question by a specific user.
	 *
	 * @param guildId the ID of the guild
	 * @param questionNumber The week's number.
	 * @param authorID The ID of the user who created the submission.
	 * @return The {@link QOTWSubmission} or {@code null} if the user has not submitted any answer to the question.
	 * @throws SQLException If an error occurs.
	 */
	public QOTWSubmission getSubmissionByQuestionNumberAndAuthorID(long guildId,int questionNumber, long authorID) throws SQLException {
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE guild_id = ? AND question_number = ? AND author_id = ?")) {
			s.setLong(1, guildId);
			s.setInt(2, questionNumber);
			s.setLong(3, authorID);
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				return this.read(rs);
			}
			return null;
		}
	}

	/**
	 * Reads a {@link ResultSet} and returns a new {@link QOTWSubmission} object.
	 *
	 * @param rs The query's ResultSet.
	 * @return The {@link QOTWSubmission} object.
	 * @throws SQLException If an error occurs.
	 */
	private QOTWSubmission read(ResultSet rs) throws SQLException {
		QOTWSubmission submission = new QOTWSubmission();
		submission.setThreadId(rs.getLong("thread_id"));
		submission.setQuestionNumber(rs.getInt("question_number"));
		submission.setGuildId(rs.getLong("guild_id"));
		submission.setAuthorId(rs.getLong("author_id"));
		submission.setStatus(SubmissionStatus.values()[rs.getInt("status")]);
		return submission;
	}
}
