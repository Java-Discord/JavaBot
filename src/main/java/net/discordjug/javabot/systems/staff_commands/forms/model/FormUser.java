package net.discordjug.javabot.systems.staff_commands.forms.model;

/**
 * Represents a user who submitted a form.
 * 
 * @param id       user's ID.
 * @param username user's Discord username.
 */
public record FormUser(long id, String username) {
}
