package net.javadiscord.javabot.command.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.jetbrains.annotations.NotNull;

/**
 * When a action cannot be completed because of a user's permissions.
 */
public class UserPermissionException extends PermissionException {
	private final User user;

	public UserPermissionException(User user, String reason) {
		super(reason);
		this.user = user;
	}

	public UserPermissionException(User user, @NotNull Permission permission) {
		super(permission);
		this.user = user;
	}

	public UserPermissionException(User user, @NotNull Permission permission, String reason) {
		super(permission, reason);
		this.user = user;
	}

	public UserPermissionException(User user, PermissionException e) {
		super(e.getPermission(), e.getMessage());
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	@Override
	public String getMessage() {
		return super.getMessage();
	}

	@Override
	public String toString() {
		return getUser().getName() + " " + super.getMessage();
	}
}
