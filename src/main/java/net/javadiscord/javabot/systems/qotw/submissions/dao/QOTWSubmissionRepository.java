package net.javadiscord.javabot.systems.qotw.submissions.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;

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
		PreparedStatement stmt = con.prepareStatement("INSERT INTO qotw_submissions (thread_id, question_number, guild_id, author_id) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, submission.getThreadId());
		stmt.setInt(2, submission.getQuestionNumber());
		stmt.setLong(3, submission.getGuildId());
		stmt.setLong(4, submission.getAuthorId());
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("Submission was not inserted.");
		stmt.close();
		log.info("Inserted new QOTW-Submission: {}", submission);
	}

	/**
	 * Removes a single {@link QOTWSubmission}.
	 *
	 * @param threadId The current thread's id.
	 * @return Whether the {@link QOTWSubmission} was actually removed.
	 * @throws SQLException If an error occurs.
	 */
	public boolean removeSubmission(long threadId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM qotw_submissions WHERE thread_id = ?");
		stmt.setLong(1, threadId);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Marks a single {@link QOTWSubmission} as reviewed.
	 *
	 * @param submission The {@link QOTWSubmission} that should be marked as reviewed.
	 * @throws SQLException If an error occurs.
	 */
	public void markReviewed(QOTWSubmission submission) throws SQLException {
		try (var stmt = con.prepareStatement("""
				UPDATE qotw_submissions
				SET reviewed = TRUE
				WHERE thread_id = ?""")) {
			stmt.setLong(1, submission.getThreadId());
			stmt.executeUpdate();
		}
	}

	/**
	 * Marks a single {@link QOTWSubmission} as accepted.
	 *
	 * @param submission The {@link QOTWSubmission} that should be marked as accepted.
	 * @throws SQLException If an error occurs.
	 */
	public void markAccepted(QOTWSubmission submission) throws SQLException {
		try (var stmt = con.prepareStatement("""
				UPDATE qotw_submissions
				SET accepted = TRUE
				WHERE thread_id = ?""")) {
			stmt.setLong(1, submission.getThreadId());
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
		PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE author_id = ? AND reviewed = FALSE AND accepted = FALSE");
		s.setLong(1, authorId);
		var rs = s.executeQuery();
		List<QOTWSubmission> submissions = new ArrayList<>();
		while (rs.next()) {
			submissions.add(read(rs));
		}
		return submissions;
	}

	/**
	 * Returns a {@link QOTWSubmission} based on the given thread id.
	 *
	 * @param threadId The discord Id of the user.
	 * @return The {@link QOTWSubmission} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWSubmission> getSubmissionByThreadId(long threadId) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE thread_id = ?");
		s.setLong(1, threadId);
		var rs = s.executeQuery();
		QOTWSubmission submission = null;
		if (rs.next()) {
			submission = read(rs);
		}
		return Optional.ofNullable(submission);
	}

	/**
	 * Returns a {@link QOTWSubmission} based on the given question number.
	 *
	 * @param questionNumber The discord Id of the user.
	 * @return The {@link QOTWSubmission} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWSubmission> getSubmissionByQuestionNumber(int questionNumber) throws SQLException {
		PreparedStatement s = con.prepareStatement("SELECT * FROM qotw_submissions WHERE question_number = ?");
		s.setInt(1, questionNumber);
		var rs = s.executeQuery();
		QOTWSubmission submission = null;
		if (rs.next()) {
			submission = read(rs);
		}
		return Optional.ofNullable(submission);
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
		submission.setReviewed(rs.getBoolean("reviewed"));
		submission.setAccepted(rs.getBoolean("accepted"));
		return submission;
	}
}
