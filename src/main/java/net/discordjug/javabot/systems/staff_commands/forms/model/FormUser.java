package net.discordjug.javabot.systems.staff_commands.forms.model;

/**
 * Represents a user who submitted a form.
 * 
 * @param id       user's ID.
 * @param username user's Discord username.
 */
public record FormUser(long id, String username) {

//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj) return true;
//		if (obj == null || getClass() != obj.getClass()) return false;
//		FormUser other = (FormUser) obj;
//		return id == other.id && Objects.equals(username, other.username);
//	}

//	@Override
//	public int hashCode() {
//		return Objects.hash(id, username);
//	}

}
