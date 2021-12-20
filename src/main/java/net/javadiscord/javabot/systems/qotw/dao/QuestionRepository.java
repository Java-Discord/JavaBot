package net.javadiscord.javabot.systems.qotw.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class QuestionRepository {
	private final Connection con;

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

	public void markUsed(QOTWQuestion question) throws SQLException {
		if (question.getQuestionNumber() == null) throw new IllegalArgumentException("Cannot mark an unnumbered question as used.");
		try (var stmt = con.prepareStatement("""
			UPDATE qotw_question
			SET used = TRUE, question_number = ?
			WHERE id = ?""")) {
			stmt.setLong(1, question.getId());
			stmt.setInt(2, question.getQuestionNumber());
			stmt.executeUpdate();
		}
	}

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
