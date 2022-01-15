package net.javadiscord.javabot.systems.expert_questions.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.expert_questions.model.ExpertQuestion;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ExpertQuestionRepository {
	private final Connection con;

	public void save(ExpertQuestion question) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO expert_questions (guild_id, text) VALUES (?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		stmt.setLong(1, question.getGuildId());
		stmt.setString(2, question.getText());
		int rows = stmt.executeUpdate();
		if (rows == 0) throw new SQLException("New question was not inserted.");
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			question.setId(rs.getLong(1));
		}
		stmt.close();
	}

	public boolean remove(long guildId, long id) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM expert_questions WHERE guild_id = ? AND id = ?");
		stmt.setLong(1, guildId);
		stmt.setLong(2, id);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	public List<ExpertQuestion> getQuestions(long guildId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT * FROM expert_questions WHERE guild_id = ?");
		stmt.setLong(1, guildId);
		ResultSet rs = stmt.executeQuery();
		List<ExpertQuestion> questions = new ArrayList<>();
		while (rs.next()) {
			questions.add(this.read(rs));
		}
		stmt.close();
		return questions;
	}

	private ExpertQuestion read(ResultSet rs) throws SQLException {
		ExpertQuestion question = new ExpertQuestion();
		question.setId(rs.getLong("id"));
		question.setGuildId(rs.getLong("guild_id"));
		question.setText(rs.getString("text"));
		return question;
	}
}
