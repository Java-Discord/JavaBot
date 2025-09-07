package net.discordjug.javabot.systems.staff_commands.forms.model;

import java.util.Objects;

/**
 * Represents an user who submitted a form.
 */
public class FormUser {
	private final long id;
	private final String username;

	/**
	 * The main constructor.
	 *
	 * @param id       user's id
	 * @param username user's username
	 */
	public FormUser(long id, String username) {
		this.id = id;
		this.username = username;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		FormUser other = (FormUser) obj;
		return id == other.id && Objects.equals(username, other.username);
	}

	public long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, username);
	}

}
