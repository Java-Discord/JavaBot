package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.entities.User;

/**
 * Utils regarding users.
 */
public class UserUtils {
    /**
     * Returns the tag of a discord user, in the format "[username]#[discriminator]", or just "[username]" if the new username system is used.
     *
     * @param user The {@link User} to get the tag of
     *
     * @return The formatted tag of the user
     */
	public static String getUserTag(User user) {
		String name = user.getName();
		String discrim = user.getDiscriminator();
		if ("0000".equals(discrim)) {
			return name;
		} else {
			return name + "#" + discrim;
		}
	}
}
