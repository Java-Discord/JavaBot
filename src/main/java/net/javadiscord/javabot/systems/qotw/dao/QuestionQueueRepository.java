package net.javadiscord.javabot.systems.qotw.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the QOTW_QUESTION SQL Table.
 */
@RequiredArgsConstructor
public class QuestionQueueRepository {
	private final Connection con;

	/**
	 * Inserts a single {@link QOTWQuestion}.
	 *
	 * @param question The {@link QOTWQuestion} to insert.
	 * @throws SQLException If an error occurs.
	 */
	public void save(QOTWQuestion question) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO qotw_question (guild_id, created_by, text, priority) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, question.getGuildId());
		stmt.setLong(2, question.getCreatedBy());
		stmt.setString(3, question.getText());
		stmt.setInt(4, question.getPriority());
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("New question was not inserted.");
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			question.setId(rs.getLong(1));
		}
		stmt.close();
	}

	/**
	 * Gets the next Question's week number.
	 *
	 * @return The next Question's week number as an integer.
	 * @throws SQLException If an error occurs.
	 */
	public int getNextQuestionNumber() throws SQLException {
		try (var stmt = con.prepareStatement("""
				SELECT question_number + 1
				FROM qotw_question
				WHERE used = TRUE AND question_number IS NOT NULL
				ORDER BY created_at DESC LIMIT 1""")) {
			var rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 1;
		}
	}

	/**
	 * Marks a single {@link QOTWQuestion} as used.
	 *
	 * @param question The {@link QOTWQuestion} that should be marked as used.
	 * @throws SQLException If an error occurs.
	 */
	public void markUsed(QOTWQuestion question) throws SQLException {
		if (question.getQuestionNumber() == null) {
			throw new IllegalArgumentException("Cannot mark an unnumbered question as used.");
		}
		try (var stmt = con.prepareStatement("""
				UPDATE qotw_question
				SET used = TRUE, question_number = ?
				WHERE id = ?""")) {
			stmt.setInt(1, question.getQuestionNumber());
			stmt.setLong(2, question.getId());
			stmt.executeUpdate();
		}
	}

	/**
	 * Gets as many Questions as specified.
	 *
	 * @param guildId The current guild's id.
	 * @param page    The page.
	 * @param size    The amount of questions to return.
	 * @return A {@link List} containing the specified amount of {@link QOTWQuestion}.
	 * @throws SQLException If an error occurs.
	 */
	public List<QOTWQuestion> getQuestions(long guildId, int page, int size) throws SQLException {
		String sql = "SELECT * FROM qotw_question WHERE guild_id = ? AND used = FALSE ORDER BY priority DESC, created_at ASC LIMIT %d OFFSET %d";
		PreparedStatement stmt = con.prepareStatement(String.format(sql, size, page));
		stmt.setLong(1, guildId);
		ResultSet rs = stmt.executeQuery();
		List<QOTWQuestion> questions = new ArrayList<>(size);
		while (rs.next()) {
			questions.add(this.read(rs));
		}
		stmt.close();
		return questions;
	}

	/**
	 * Retrieves the next question.
	 *
	 * @param guildId The current guild's id.
	 * @return The next {@link QOTWQuestion} as an {@link Optional}
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWQuestion> getNextQuestion(long guildId) throws SQLException {
		try (var stmt = con.prepareStatement("""
				SELECT *
				FROM qotw_question
				WHERE guild_id = ? AND used = FALSE
				ORDER BY priority DESC, created_at
				LIMIT 1""")) {
			stmt.setLong(1, guildId);
			ResultSet rs = stmt.executeQuery();
			Optional<QOTWQuestion> optionalQuestion;
			if (rs.next()) {
				optionalQuestion = Optional.of(this.read(rs));
			} else {
				optionalQuestion = Optional.empty();
			}
			return optionalQuestion;
		}
	}

	/**
	 * Removes a single {@link QOTWQuestion}.
	 *
	 * @param guildId The current guild's id.
	 * @param id      The question's id.
	 * @return Whether the {@link QOTWQuestion} was actually removed.
	 * @throws SQLException If an error occurs.
	 */
	public boolean removeQuestion(long guildId, long id) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM qotw_question WHERE guild_id = ? AND id = ?");
		stmt.setLong(1, guildId);
		stmt.setLong(2, id);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	private QOTWQuestion read(ResultSet rs) throws SQLException {
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
