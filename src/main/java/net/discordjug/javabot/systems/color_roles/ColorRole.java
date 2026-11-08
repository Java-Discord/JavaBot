package net.discordjug.javabot.systems.color_roles;

import java.awt.Color;
import java.util.Objects;

/**
 * A self-assignable color role.
 * @param guildId The Discord ID of the guild the role is a part of
 * @param roleId The Discord ID of the role
 * @param color The color of the role
 * @see ChangeColorCommand
 * @see ColorRoleService
 */
public record ColorRole(long guildId, long roleId, Color color) {
	public ColorRole {
		Objects.requireNonNull(color);
	}
}