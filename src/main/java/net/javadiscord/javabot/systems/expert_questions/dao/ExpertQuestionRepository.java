package net.javadiscord.javabot.systems.expert_questions.dao;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.expert_questions.model.ExpertQuestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dao class that represents the EXPERT_QUESTIONS SQL Table.
 */
@RequiredArgsConstructor
public class ExpertQuestionRepository {
	private final Connection con;

	/**
	 * Inserts a single {@link ExpertQuestion}.
	 * @param question The {@link ExpertQuestion} object to insert.
	 * @throws SQLException If an error occurs.
	 */
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

	/**
	 * Removes a single {@link ExpertQuestion}.
	 * @param guildId The current guild's id.
	 * @param id The question's id.
	 * @return Whether the question was removed or not.
	 * @throws SQLException If an error occurs.
	 */
	public boolean remove(long guildId, long id) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM expert_questions WHERE guild_id = ? AND id = ?");
		stmt.setLong(1, guildId);
		stmt.setLong(2, id);
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows > 0;
	}

	/**
	 * Gets all expert questions for the given guild.
	 * @param guildId The guild's id.
	 * @return A {@link List} with all Expert Questions.
	 * @throws SQLException If an error occurs.
	 */
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
