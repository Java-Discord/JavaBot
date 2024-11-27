package net.discordjug.javabot.systems.custom_vc;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Stores currently active custom voice channels and the owners of these channels.
 */
@Repository
@RequiredArgsConstructor
public class CustomVCRepository {
	
	private final JdbcTemplate jdbcTemplate;
	
	/**
	 * Stores a new custom voice channel.
	 * @param id the channel ID
	 * @param ownerId the ID of the owner of the voice channel
	 */
	public void addCustomVoiceChannel(long id, long ownerId) {
		jdbcTemplate.update("INSERT INTO custom_vc (channel_id, owner_id) VALUES (?, ?)", 
				id, ownerId);
	}
	
	/**
	 * Removes a custom voice channel.
	 * @param id the channel ID
	 */
	public void removeCustomVoiceChannel(long id) {
		jdbcTemplate.update("DELETE FROM custom_vc WHERE channel_id = ?", id);
	}
	
	/**
	 * Checks whether a channel is a custom voice channel.
	 * @param id the channel ID
	 * @return {@code true} if the channel is a custom voice channel, else {@code false}
	 */
	public boolean isCustomVoiceChannel(long id) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM custom_vc WHERE channel_id = ?", (rs, rowId) -> rs.getInt(1), id) > 0;
	}
	
	/**
	 * Gets the owner of a custom voice channel.
	 * @param voiceChannelId the ID of the voice channel
	 * @return the ID of the owner of the custom voice channel
	 */
	public long getOwnerId(long voiceChannelId) {
		return jdbcTemplate.queryForObject("SELECT owner_id FROM custom_vc WHERE channel_id = ?",
				(rs, rowId) -> rs.getLong(1),
				voiceChannelId);
	}
	
	/**
	 * Gets all custom voice channels of all guilds.
	 * @return a {@link List} of all custom voice channel IDs
	 */
	public List<Long> getAllCustomVoiceChannels() {
		return jdbcTemplate.query("SELECT channel_id FROM custom_vc", (rs, row) -> rs.getLong(1));
	}
}
