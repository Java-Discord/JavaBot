package net.javadiscord.javabot.api.routes.user_profile.model;

import lombok.Data;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;

import java.util.List;

/**
 * API-Data class which contains all necessary information about a users'
 * profile.
 */
@Data
public class UserProfileData {
	private long userId;
	private String userName;
	private String discriminator;
	private String effectiveAvatarUrl;
	private QOTWAccount qotwAccount;
	private HelpAccountData helpAccount;
	private List<UserPreference> preferences;
	private List<Warn> warns;
}
