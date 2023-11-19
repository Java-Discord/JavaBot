package net.discordjug.javabot.systems.qotw.dao;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.systems.qotw.model.QOTWQuestion;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

/**
 * Dao class that represents the QOTW_QUESTION SQL Table.
 */
@RequiredArgsConstructor
@Repository
public class QuestionQueueRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Inserts a single {@link QOTWQuestion}.
	 *
	 * @param question The {@link QOTWQuestion} to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void save(QOTWQuestion question) throws DataAccessException {
		Number key = new SimpleJdbcInsert(jdbcTemplate)
		.withTableName("qotw_question")
		.usingColumns("guild_id","created_by","text","priority")
		.usingGeneratedKeyColumns("id")
		.executeAndReturnKey(Map.of(
				"guild_id",question.getGuildId(),
				"created_by",question.getCreatedBy(),
				"text",question.getText(),
				"priority",question.getPriority()
				));
		question.setId(key.longValue());
	}

	/**
	 * Gets a {@link QOTWQuestion} by its Question Number.
	 *
	 * @param questionNumber The question's number.
	 * @return The question as an {@link Optional}
	 * @throws DataAccessException If an error occurs.
	 */
	public Optional<QOTWQuestion> findByQuestionNumber(int questionNumber) throws DataAccessException {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT * FROM qotw_question WHERE question_number = ?", (rs, row)->this.read(rs),
					questionNumber));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the next Question's week number.
	 *
	 * @return The next Question's week number as an integer.
	 * @throws SQLException If an error occurs.
	 */
	public int getNextQuestionNumber() throws DataAccessException {
		try {
			return jdbcTemplate.queryForObject("""
					SELECT question_number + 1
					FROM qotw_question
					WHERE used = TRUE AND question_number IS NOT NULL
					ORDER BY created_at DESC LIMIT 1""", (rs, row)->rs.getInt(1));
		} catch (EmptyResultDataAccessException e) {
			return 1;
		}
	}

	/**
	 * Marks a single {@link QOTWQuestion} as used.
	 *
	 * @param question The {@link QOTWQuestion} that should be marked as used.
	 * @throws DataAccessException If an error occurs.
	 */
	public void markUsed(@NotNull QOTWQuestion question) throws DataAccessException {
		if (question.getQuestionNumber() == null) {
			throw new IllegalArgumentException("Cannot mark an unnumbered question as used.");
		}
		jdbcTemplate.update("""
				UPDATE qotw_question
				SET used = TRUE, question_number = ?
				WHERE id = ?""",
				question.getQuestionNumber(), question.getId());
	}

	/**
	 * Gets as many Questions as specified.
	 *
	 * @param guildId The current guild's id.
	 * @param page    The page.
	 * @param size    The amount of questions to return.
	 * @return A {@link List} containing the specified amount of {@link QOTWQuestion}.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<QOTWQuestion> getQuestions(long guildId, int page, int size) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM qotw_question WHERE guild_id = ? AND used = FALSE ORDER BY priority DESC, created_at ASC LIMIT ? OFFSET ?", (rs, row)-> this.read(rs),
				guildId, size, page);
	}

	/**
	 * Gets as many questions matching a query as specified.
	 * @param guildId The current guild's id..
	 * @param query   The query to match questions against.
	 * @param page    The page.
	 * @param size    The amount of questions to return.
	 * @return A {@link List} containing the specified amount (or less) of {@link QOTWQuestion} matching the query.
	 * @throws DataAccessException If an error occurs.
	 */
	public List<QOTWQuestion> getUsedQuestionsWithQuery(long guildId, String query, int page, int size) throws DataAccessException {
		return jdbcTemplate.query("SELECT * FROM qotw_question WHERE guild_id = ? AND \"TEXT\" LIKE ? AND used = TRUE ORDER BY question_number DESC, created_at ASC LIMIT ? OFFSET ?", (rs, rows)->this.read(rs),
				guildId, "%" + query.toLowerCase() + "%", size, page);
	}

	/**
	 * Retrieves the next question.
	 *
	 * @param guildId The current guild's id.
	 * @return The next {@link QOTWQuestion} as an {@link Optional}
	 * @throws DataAccessException If an error occurs.
	 */
	public Optional<QOTWQuestion> getNextQuestion(long guildId) throws DataAccessException {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject("""
					SELECT *
					FROM qotw_question
					WHERE guild_id = ? AND used = FALSE
					ORDER BY priority DESC, created_at
					LIMIT 1""",
					(rs, rows)->this.read(rs),
					guildId));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Removes a single {@link QOTWQuestion}.
	 *
	 * @param guildId The current guild's id.
	 * @param id      The question's id.
	 * @return Whether the {@link QOTWQuestion} was actually removed.
	 * @throws DataAccessException If an error occurs.
	 */
	public boolean removeQuestion(long guildId, long id) throws DataAccessException {
		return jdbcTemplate.update("DELETE FROM qotw_question WHERE guild_id = ? AND id = ?",
				guildId, id) > 0;
	}

	private @NotNull QOTWQuestion read(@NotNull ResultSet rs) throws SQLException {
		QOTWQuestion question = new QOTWQuestion();
		question.setId(rs.getLong("id"));
		question.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		question.setGuildId(rs.getLong("guild_id"));
		question.setCreatedBy(rs.getLong("created_by"));
		question.setText(rs.getString("text"));
		question.setUsed(rs.getBoolean("used"));
		int questionNumber = rs.getInt("question_number");
		question.setQuestionNumber(rs.wasNull() ? null : questionNumber);
		question.setPriority(rs.getInt("priority"));
		return question;
	}
}
