package net.discordjug.javabot.systems.staff_commands.forms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormField;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormUser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

/**
 * Dao class that represents the FORMS table.
 */
@RequiredArgsConstructor
@Repository
public class FormsRepository {
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Add a field to a form.
	 *
	 * @param form  form to add field to
	 * @param field field to add
	 */
	public void addField(FormData form, FormField field) {
		jdbcTemplate.update(
				"INSERT INTO FORM_FIELDS (FORM_ID, LABEL, MIN, MAX, PLACEHOLDER, REQUIRED, \"style\", INITIAL) "
						+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?)",
				form.getId(), field.getLabel(), field.getMin(), field.getMax(), field.getPlaceholder(),
				field.isRequired(), field.getStyle().name(), field.getValue());
	}

	/**
	 * Attaches a form to a message.
	 *
	 * @param form    form to attach
	 * @param message message to attach the form to
	 * @param channel channel of the message
	 */
	public void attachForm(FormData form, MessageChannel channel, Message message) {
		Objects.requireNonNull(form);
		Objects.requireNonNull(channel);
		Objects.requireNonNull(message);
		jdbcTemplate.update("update `forms` set `message_id` = ?, `message_channel` = ? where `form_id` = ?",
				message.getId(), channel.getId(), form.getId());
	}

	/**
	 * Set this form's closed state to true.
	 *
	 * @param form form to close
	 */
	public void closeForm(FormData form) {
		jdbcTemplate.update("update `forms` set `closed` = true where `form_id` = ?", form.getId());
	}

	/**
	 * Deletes a form from the database.
	 *
	 * @param form form to delete
	 */
	public void deleteForm(FormData form) {
		jdbcTemplate.update("delete from `forms` where `form_id` = ?", form.getId());
	}

	/**
	 * Deletes user's submissions from this form.
	 *
	 * @param form form to delete submissions for
	 * @param user user to delete submissions for
	 * @return number of deleted submissions
	 */
	public int deleteSubmissions(FormData form, User user) {
		Objects.requireNonNull(form);
		Objects.requireNonNull(user);
		return jdbcTemplate.update("delete from `form_submissions` where `form_id` = ? and `user_id` = ?", form.getId(),
				user.getIdLong());
	}

	/**
	 * Detaches a form from a message.
	 *
	 * @param form form to detach
	 */
	public void detachForm(FormData form) {
		Objects.requireNonNull(form);
		jdbcTemplate.update("update `forms` set `message_id` = NULL, `message_channel` = NULL where `form_id` = ?",
				form.getId());
	}

	/**
	 * Get all forms from the database.
	 *
	 * @return A list of forms
	 */
	public List<FormData> getAllForms() {
		return jdbcTemplate.query("select * from `forms`", (rs, rowNum) -> read(rs, readFormFields(rowNum)));
	}

	/**
	 * Get all forms matching given closed state.
	 *
	 * @param closed the closed state
	 * @return A list of forms matching the closed state
	 */
	public List<FormData> getAllForms(boolean closed) {
		return jdbcTemplate.query(con -> {
			PreparedStatement statement = con.prepareStatement("select * from `forms` where `closed` = ?");
			statement.setBoolean(1, closed);
			return statement;
		}, (rs, rowNum) -> read(rs, readFormFields(rowNum)));
	}

	/**
	 * Get all submissions of this form in an user -> count map.
	 *
	 * @param form a form to get submissions for
	 * @return a map of users and the number of their submissions
	 */
	public Map<FormUser, Integer> getAllSubmissions(FormData form) {
		Objects.requireNonNull(form);
		List<FormUser> users = jdbcTemplate.query("select * from `form_submissions` where `form_id` = ?",
				(rs, rowNum) -> new FormUser(rs.getLong("user_id"), rs.getString("user_name")), form.getId());
		Map<FormUser, Integer> map = new HashMap<>();
		for (FormUser user : users) {
			map.compute(user, (t, u) -> u == null ? 1 : u + 1);
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Get a form for given ID.
	 *
	 * @param formId form ID to query
	 * @return optional containing the form, or empty if the form was not found.
	 */
	public Optional<FormData> getForm(long formId) {
		try {
			return Optional.of(jdbcTemplate.queryForObject("select * from `forms` where `form_id` = ?",
					(RowMapper<FormData>) (rs, rowNum) -> read(rs, readFormFields(formId)), formId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	/**
	 * Get a count of logged submissions for the given form.
	 *
	 * @param form form to get submission for
	 * @return A total number of logged submission
	 */
	public int getTotalSubmissionsCount(FormData form) {
		Objects.requireNonNull(form);
		return jdbcTemplate.queryForObject("select count(*) from `form_submissions` where `form_id` = ?",
				(rs, rowNum) -> rs.getInt(1), form.getId());
	}

	/**
	 * Checks if an user already submitted the form.
	 *
	 * @param user user to check
	 * @param form form to check on
	 * @return true if the user has submitted at leas one submission, false
	 *         otherwise
	 */
	public boolean hasSubmitted(User user, FormData form) {
		try {
			return jdbcTemplate.queryForObject(
					"select * from `form_submissions` where `user_id` = ? and `form_id` = ? limit 1",
					(rs, rowNum) -> true, user.getIdLong(), form.getId());
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}

	/**
	 * Create a new form entry in the database.
	 *
	 * @param data form data to insert.
	 */
	public void insertForm(@NonNull FormData data) {
		Objects.requireNonNull(data);
		jdbcTemplate.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement statement = con.prepareStatement(
						"insert into `forms` (title, submit_message, submit_channel, message_id, message_channel, expiration, onetime) values (?, ?, ?, ?, ?, ?, ?)");
				statement.setString(1, data.getTitle());
				statement.setString(2, data.getSubmitMessage());
				statement.setLong(3, data.getSubmitChannel());
				statement.setObject(4, data.getMessageId().orElse(null));
				statement.setObject(5, data.getMessageChannel().orElse(null));
				statement.setLong(6, data.getExpiration());
				statement.setBoolean(7, data.isOnetime());
				return statement;
			}
		});
	}

	/**
	 * Log an user form submission in database.
	 *
	 * @param user    user to log
	 * @param form    form to log on
	 * @param message message containing details about this user's submission
	 */
	public void logSubmission(User user, FormData form, Message message) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(form);
		jdbcTemplate.update(con -> {
			PreparedStatement statement = con.prepareStatement(
					"insert into `form_submissions` (`message_id`, `user_id`, `form_id`, `user_name`) values (?, ?, ?, ?)");
			statement.setLong(1, message.getIdLong());
			statement.setLong(2, user.getIdLong());
			statement.setLong(3, form.getId());
			statement.setString(4, user.getName());
			return statement;
		});
	}

	/**
	 * Remove a field from a form. Fails silently if the index is out of bounds.
	 *
	 * @param form  form to remove the field from
	 * @param index index of the field to remove
	 */
	public void removeField(FormData form, int index) {
		List<FormField> fields = form.getFields();
		if (index < 0 || index >= fields.size()) return;
		jdbcTemplate.update("delete from `form_fields` where `id` = ?", fields.get(index).getId());
	}

	/**
	 * Set this form's closed state to false.
	 *
	 * @param form form to re-open
	 */
	public void reopenForm(FormData form) {
		jdbcTemplate.update("update `forms` set `closed` = false where `form_id` = ?", form.getId());
	}

	/**
	 * Synchronizes form object's values with fields in database.
	 *
	 * @param newData new form data. A form with matching ID will be updated in the
	 *                database.
	 */
	public void updateForm(FormData newData) {
		Objects.requireNonNull(newData);
		jdbcTemplate.update(con -> {
			PreparedStatement statement = con.prepareStatement(
					"update `forms` set `title` = ?, `submit_channel` = ?, `submit_message` = ?, `expiration` = ?, `onetime` = ? where `form_id` = ?");
			statement.setString(1, newData.getTitle());
			statement.setLong(2, newData.getSubmitChannel());
			statement.setString(3, newData.getSubmitMessage());
			statement.setLong(4, newData.getExpiration());
			statement.setBoolean(5, newData.isOnetime());
			statement.setLong(6, newData.getId());
			return statement;
		});
	}

	private List<FormField> readFormFields(long formId) {
		return jdbcTemplate.query("select * from `form_fields` where `form_id` = ?", (rs, rowNum) -> readField(rs),
				formId);
	}

	private static FormData read(ResultSet rs, List<FormField> fields) throws SQLException {
		Long messageId = rs.getLong("message_id");
		if (rs.wasNull()) messageId = null;
		Long messageChannel = rs.getLong("message_channel");
		if (rs.wasNull()) messageChannel = null;
		return new FormData(rs.getLong("form_id"), fields, rs.getString("title"), rs.getLong("submit_channel"),
				rs.getString("submit_message"), messageId, messageChannel, rs.getLong("expiration"),
				rs.getBoolean("closed"), rs.getBoolean("onetime"));
	}

	private static FormField readField(ResultSet rs) throws SQLException {
		return new FormField(rs.getString("label"), rs.getInt("max"), rs.getInt("min"), rs.getString("placeholder"),
				rs.getBoolean("required"), TextInputStyle.valueOf(rs.getString("style").toUpperCase()),
				rs.getString("initial"), rs.getInt("id"));
	}
}
