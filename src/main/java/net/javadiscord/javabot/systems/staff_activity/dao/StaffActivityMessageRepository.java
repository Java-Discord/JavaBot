package net.javadiscord.javabot.systems.staff_activity.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.staff_activity.model.StaffActivityMessage;

/**
 * Repository for storing staff activity message locations.
 */
@RequiredArgsConstructor
@Repository
public class StaffActivityMessageRepository {
	private final JdbcTemplate jdbcTemplate;
	
	/**
	 * Inserts a new {@link StaffActivityMessage} or replaces an old one.
	 * @param msg the {@link StaffActivityMessage} to store
	 */
	public void insertOrReplace(StaffActivityMessage msg) {
		jdbcTemplate.update("""
				MERGE INTO staff_activity_messages
					(guild_id, user_id, message_id)
				KEY	(guild_id, user_id)
				VALUES
					(?,?,?)
				""", msg.guildId(), msg.userId(), msg.messageId());
	}
	
	/**
	 * gets the ID of the activity message of a specific staff member.
	 * @param guildId the ID of the relevant guild
	 * @param userId the ID of the staff member
	 * @return the message ID of the activity message
	 */
	public Long getMessageId(long guildId, long userId) {
		return jdbcTemplate.query("SELECT message_id FROM staff_activity_messages WHERE guild_id=? AND user_id=?", rs-> rs.next() ? (Long)rs.getLong(1) : null, guildId, userId);
	}
}
