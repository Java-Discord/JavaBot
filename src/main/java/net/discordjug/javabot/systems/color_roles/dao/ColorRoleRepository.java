package net.discordjug.javabot.systems.color_roles.dao;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.systems.color_roles.ColorRole;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository class for the COLOR_ROLES and COLOR_ROLE_MEMBERS tables which are used for role colors.
 */
@Repository
@RequiredArgsConstructor
public class ColorRoleRepository {
	private final JdbcTemplate jdbcTemplate;
	
	/**
	 * Gets all color roles of a guild.
	 * @param guildId The ID of the guild
	 * @return A {@link List} of all registered role colors in the guild
	 */
	public List<ColorRole> getColorRoles(long guildId) {
		return jdbcTemplate.query("SELECT ROLE_ID, COLOR FROM COLOR_ROLES WHERE GUILD_ID=?",
				(rs, _) -> new ColorRole(guildId, rs.getLong(1), new Color(rs.getInt(2))),
				guildId);
	}
	
	/**
	 * Tries to find a color role given the color.
	 * @param guildId The ID of the guild containing the role
	 * @param color The color of the role to search for
	 * @return The color role if it exists or {@link Optional#empty()}
	 */
	public Optional<ColorRole> getFromColor(long guildId, Color color) {
		try {
			ColorRole result = jdbcTemplate.queryForObject("SELECT ROLE_ID FROM COLOR_ROLES WHERE GUILD_ID=? AND COLOR=?",
					(rs, _) -> new ColorRole(guildId, rs.getLong(1), color),
					guildId, color.getRGB());
			return Optional.ofNullable(result);
		} catch(EmptyResultDataAccessException _) {
			return Optional.empty();
		}
	}
	
	/**
	 * Gets all color roles of a given member.
	 * @param guildId The ID of the guild the roles should be part of
	 * @param memberId The ID of the member get the roles of
	 * @return All color roles of the given member in the given guild
	 */
	public List<ColorRole> getColorRolesOfMember(long guildId, long memberId) {
		return jdbcTemplate.query("""
				SELECT cr.ROLE_ID, cr.COLOR
				FROM COLOR_ROLES cr
				INNER JOIN COLOR_ROLE_MEMBERS crm ON cr.ROLE_ID = crm.ROLE_ID
				WHERE cr.GUILD_ID=? AND crm.USER_ID=?
				""",
				(rs, _) -> new ColorRole(guildId, rs.getLong(1), new Color(rs.getInt(2))),
				guildId, memberId);
	}
	
	/**
	 * Registers that a member has a given color role.
	 * @param color The color role
	 * @param memberId The member the role is being added to
	 */
	public void addColorRole(ColorRole color, long memberId) {
		jdbcTemplate.update("INSERT INTO COLOR_ROLES (ROLE_ID, GUILD_ID, COLOR) VALUES (?,?,?)",
				color.roleId(), color.guildId(), color.color().getRGB());
		addRoleToMember(color.roleId(), memberId);
	}
	
	/**
	 * Registers that a color role has been added to a member.
	 * @param roleId The ID of the role the member has
	 * @param memberId The ID of the member to update
	 */
	public void addRoleToMember(long roleId, long memberId) {
		jdbcTemplate.update("INSERT INTO COLOR_ROLE_MEMBERS (ROLE_ID, USER_ID) VALUES (?,?)", roleId, memberId);
	}
	
	/**
	 * Registers that a color role has been added to a member.
	 * @param roleId The ID of the role removed from the member
	 * @param memberId The ID of the member to update
	 * @see #cleanRoleIfNoMembers(long)
	 */
	public void removeRoleFromMember(long roleId, long memberId) {
		jdbcTemplate.update("DELETE FROM COLOR_ROLE_MEMBERS WHERE ROLE_ID=? AND USER_ID=?", roleId, memberId);
	}
	
	/**
	 * Removes a role from the database if it has no members.
	 * 
	 * Callers should make sure to actually delete the role if this method returns {@code true}.
	 * @param roleId The ID of the role to possibly remove
	 * @return {@code true} if the role is registered as removed due to not having any members, else {@code false}
	 */
	@CheckReturnValue
	public boolean cleanRoleIfNoMembers(long roleId) {
		return jdbcTemplate.update("DELETE FROM COLOR_ROLES WHERE ROLE_ID=? AND (SELECT COUNT(*) FROM COLOR_ROLE_MEMBERS WHERE ROLE_ID=?)=0", roleId, roleId) > 0;
	}
}
