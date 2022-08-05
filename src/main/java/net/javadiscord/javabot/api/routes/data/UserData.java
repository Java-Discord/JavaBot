package net.javadiscord.javabot.api.routes.data;

import lombok.Data;

/**
 * Abstract class which contains basic user values.
 */
@Data
public abstract class UserData {
	private long userId;
	private String userName;
	private String discriminator;
	private String effectiveAvatarUrl;
}
