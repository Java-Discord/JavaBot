package net.javadiscord.javabot.systems.qotw.dao;

import net.javadiscord.javabot.data.h2db.DatabaseRepository;
import net.javadiscord.javabot.data.h2db.TableProperty;
import net.javadiscord.javabot.systems.qotw.model.QOTWQuestion;
import org.h2.api.H2Type;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Dao class that represents the QOTW_QUESTION SQL Table.
 */
public class QuestionQueueRepository extends DatabaseRepository<QOTWQuestion> {
	private final Connection con;

	/**
	 * The constructor of this {@link DatabaseRepository} class which defines all important information
	 * about the QOTW_QUESTION database table.
	 *
	 * @param con The {@link Connection} to use.
	 */
	public QuestionQueueRepository(Connection con) {
		super(con, QOTWQuestion.class, "QOTW_QUESTION", List.of(
				TableProperty.of("id",              H2Type.BIGINT,  (x, y) -> x.setId((Long) y),                    QOTWQuestion::getId, true),
				TableProperty.of("created_at",      H2Type.BIGINT,  (x, y) -> x.setCreatedAt((LocalDateTime) y),    QOTWQuestion::getCreatedAt),
				TableProperty.of("guild_id",        H2Type.BIGINT,  (x, y) -> x.setGuildId((Long) y),               QOTWQuestion::getGuildId),
				TableProperty.of("created_by",      H2Type.BIGINT,  (x, y) -> x.setCreatedBy((Long) y),             QOTWQuestion::getCreatedBy),
				TableProperty.of("text",            H2Type.VARCHAR, (x, y) -> x.setText((String) y),                QOTWQuestion::getText),
				TableProperty.of("used",            H2Type.BOOLEAN, (x, y) -> x.setUsed((Boolean) y),               QOTWQuestion::isUsed),
				TableProperty.of("question_number", H2Type.INTEGER, (x, y) -> x.setQuestionNumber((Integer) y),     QOTWQuestion::getQuestionNumber),
				TableProperty.of("priority",        H2Type.INTEGER, (x, y) -> x.setPriority((Integer) y),           QOTWQuestion::getPriority)
		));
		this.con = con;
	}

	/**
	 * Gets a {@link QOTWQuestion} by its Question Number.
	 *
	 * @param questionNumber The question's number.
	 * @return The question as an {@link Optional}
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWQuestion> findByQuestionNumber(int questionNumber) throws SQLException {
		return querySingle("WHERE question_number = ?", questionNumber);
	}

	/**
	 * Gets the next Question's week number.
	 *
	 * @return The next Question's week number as an integer.
	 * @throws SQLException If an error occurs.
	 */
	public int getNextQuestionNumber() throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("""
				SELECT question_number + 1
				FROM qotw_question
				WHERE used = TRUE AND question_number IS NOT NULL
				ORDER BY created_at DESC LIMIT 1""")) {
			ResultSet rs = stmt.executeQuery();
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
	 * @return Whether the operation was successful.
	 * @throws SQLException If an error occurs.
	 */
	public boolean markUsed(@NotNull QOTWQuestion question) throws SQLException {
		if (question.getQuestionNumber() == null) {
			throw new IllegalArgumentException("Cannot mark an unnumbered question as used.");
		}
		return update("UPDATE qotw_question SET used = TRUE, question_numer = ? WHERE id = ?", question.getQuestionNumber(), question.getId()) > 0;
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
		return queryMultiple(String.format("WHERE guild_id = ? AND used = FALSE ORDER BY priority DESC, created_at ASC LIMIT %d OFFSET %d", size, page), guildId);
	}

	/**
	 * Retrieves the next question.
	 *
	 * @param guildId The current guild's id.
	 * @return The next {@link QOTWQuestion} as an {@link Optional}
	 * @throws SQLException If an error occurs.
	 */
	public Optional<QOTWQuestion> getNextQuestion(long guildId) throws SQLException {
		return querySingle("WHERE guild_id = ? AND used = FALSE ORDER BY priority DESC, created_at LIMIT 1", guildId);
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
		try (PreparedStatement stmt = con.prepareStatement("DELETE FROM qotw_question WHERE guild_id = ? AND id = ?")) {
			stmt.setLong(1, guildId);
			stmt.setLong(2, id);
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}
}
