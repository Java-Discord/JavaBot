package net.javadiscord.javabot.systems.qotw.submissions.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.systems.qotw.submissions.SubmissionStatus;
import net.javadiscord.javabot.systems.qotw.submissions.model.QOTWSubmission;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the QOTW_SUBMISSIONS SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class QOTWSubmissionRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a new {@link QOTWSubmission}.
	 *
	 * @param submission The account to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void insert(QOTWSubmission submission) throws DataAccessException {
		int rows = jdbcTemplate.update("INSERT INTO qotw_submissions (thread_id, question_number, guild_id, author_id) VALUES (?, ?, ?, ?)",
				submission.getThreadId(),submission.getQuestionNumber(),submission.getGuildId(),submission.getAuthorId());
		if (rows == 0) throw new DataAccessException("Submission was not inserted.") {};
		log.info("Inserted new QOTW-Submission: {}", submission);
	}

	/**
	 * Removes a single {@link QOTWSubmission}.
	 *
	 * @param threadId The current thread's id.
	 * @return Whether the {@link QOTWSubmission} was actually removed.
	 * @throws SQLException If an error occurs.
	 */
	public boolean deleteSubmission(long threadId) throws DataAccessException {
		return jdbcTemplate.update("DELETE FROM qotw_submissions WHERE thread_id = ?",
				threadId) > 0;
	}

	/**
	 * Updates the status of a single {@link QOTWSubmission}.
	 *
	 * @param threadId The submission's thread id.
	 * @param status The new {@link SubmissionStatus}.
	 * @throws SQLException If an error occurs.
	 */
	public void updateStatus(long threadId, @NotNull SubmissionStatus status) throws DataAccessException {
		jdbcTemplate.update("UPDATE qotw_submissions SET status = ? WHERE thread_id = ?",
				status.ordinal(),threadId);
	}

	/**
	 * Gets all unreviewed {@link QOTWSubmission}'s from a single user.
	 *
	 * @param authorId The user's id.
	 * @return A List of unreviewed {@link QOTWSubmission}s.
	 * @throws SQLException If an error occurs.
	 */
	public List<QOTWSubmission> getUnreviewedSubmissions(long authorId) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM qotw_submissions WHERE author_id = ? AND status = 0", (rs,row)->this.read(rs),
				authorId);
	}

	/**
	 * Returns a {@link QOTWSubmission} based on the given thread id.
	 *
	 * @param threadId The discord Id of the user.
	 * @return The {@link QOTWSubmission} object.
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWSubmission> getSubmissionByThreadId(long threadId) throws DataAccessException {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM qotw_submissions WHERE thread_id = ?", (rs, row)->this.read(rs),
					threadId));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Returns the current Question Number.
	 *
	 * @return The current Question Number.
	 * @throws SQLException If an error occurs.
	 */
	public int getCurrentQuestionNumber() throws DataAccessException {
		try {
			return jdbcTemplate.queryForObject("SELECT MAX(question_number) FROM qotw_submissions",(rs, row)->rs.getInt(1));
		}catch (EmptyResultDataAccessException e) {
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
	public List<QOTWSubmission> getSubmissionsByQuestionNumber(long guildId, int questionNumber) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM qotw_submissions WHERE guild_id = ? AND question_number = ?", (rs, row)->this.read(rs),
				guildId, questionNumber);
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
	public QOTWSubmission getSubmissionByQuestionNumberAndAuthorID(long guildId,int questionNumber, long authorID) throws DataAccessException {
		try{
			return jdbcTemplate.queryForObject("SELECT * FROM qotw_submissions WHERE guild_id = ? AND question_number = ? AND author_id = ?", (rs, row)->this.read(rs),
				guildId, questionNumber, authorID);
		}catch (EmptyResultDataAccessException e) {
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
