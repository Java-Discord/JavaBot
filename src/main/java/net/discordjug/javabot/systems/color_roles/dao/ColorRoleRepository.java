package net.discordjug.javabot.systems.color_roles.dao;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.systems.color_roles.ColorRole;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ColorRoleRepository {
	private final JdbcTemplate jdbcTemplate;
	
	public List<ColorRole> getColorRoles(long guildId) {
		return jdbcTemplate.query("SELECT ROLE_ID, COLOR FROM COLOR_ROLES WHERE GUILD_ID=?",
				(rs, _) -> new ColorRole(guildId, rs.getLong(1), new Color(rs.getInt(2))),
				guildId);
	}
	
	public Optional<ColorRole> getFromColor(long guildId, Color color) {
		try {
			ColorRole result = jdbcTemplate.queryForObject("SELECT ROLE_ID FROM COLOR_ROLES WHERE GUILD_ID=? AND COLOR=?",
					((rs, _) -> new ColorRole(guildId, rs.getLong(1), color)),
					guildId, color.getRGB());
			return Optional.ofNullable(result);
		} catch(EmptyResultDataAccessException _) {
			return Optional.empty();
		}
	}
	
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
	
	public void addColorRole(ColorRole color, long memberId) {
		jdbcTemplate.update("INSERT INTO COLOR_ROLES (ROLE_ID, GUILD_ID, COLOR) VALUES (?,?,?)",
				color.roleId(), color.guildId(), color.color().getRGB());
		jdbcTemplate.update("INSERT INTO COLOR_ROLE_MEMBERS (ROLE_ID, USER_ID) VALUES (?, ?)", color.roleId(), memberId);
	}
	
	public void addRoleToMember(long roleId, long memberId) {
		jdbcTemplate.update("INSERT INTO COLOR_ROLE_MEMBERS (ROLE_ID, USER_ID) VALUES (?,?)", roleId, memberId);
	}
	public void removeRoleFromMember(long roleId, long memberId) {
		jdbcTemplate.update("DELETE FROM COLOR_ROLE_MEMBERS WHERE ROLE_ID=? AND USER_ID=?", roleId, memberId);
	}
	
	public boolean cleanRoleIfNoMembers(long roleId) {
		return jdbcTemplate.update("DELETE FROM COLOR_ROLES WHERE ROLE_ID=? AND (SELECT COUNT(*) FROM COLOR_ROLE_MEMBERS WHERE ROLE_ID=?)=0", roleId, roleId) > 0;
	}
}
