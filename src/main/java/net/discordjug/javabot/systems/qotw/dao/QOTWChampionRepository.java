package net.discordjug.javabot.systems.qotw.dao;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository for the qotw_champion table.
 * This table is used for storing the current QOTW champions.
 */
@RequiredArgsConstructor
@Repository
public class QOTWChampionRepository {
	private final JdbcTemplate jdbcTemplate;
	
	/**
	 * Gets the currently configured QOTW champions.
	 * @param guildId the ID of the guild to get the champions
	 * @return a {@link List} containing the Discord user IDs of the current QOTW champions
	 */
	public List<Long> getCurrentQOTWChampions(long guildId) {
		return jdbcTemplate.query("SELECT user_id FROM qotw_champion WHERE guild_id = ?",
				(rs, row)->{
					return rs.getLong(1);
				},
				guildId);
	}
	
	/**
	 * Sets the current QOTW champions for a specific guild.
	 * @param guild the guild to set the QOTW champions in
	 * @param users the QOTW champions
	 */
	public void setCurrentQOTWChampions(long guild, long[] users) {
		jdbcTemplate.update("DELETE FROM qotw_champion WHERE guild_id = ?");
		List<Object[]> params = new ArrayList<>();
		for (long userId : users) {
			params.add(new Object[] {guild, userId});
		}
		jdbcTemplate.batchUpdate("INSERT INTO qotw_champion (guild_id, user_id) VALUES (?,?)", params);
	}
}
