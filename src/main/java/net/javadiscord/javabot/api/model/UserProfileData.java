package net.javadiscord.javabot.api.model;

import lombok.Data;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
import net.javadiscord.javabot.systems.user_preferences.model.UserPreference;

import java.util.List;

@Data
public class UserProfileData {
	private long userId;
	private String avatarUrl;
	private QOTWAccount qotwAccount;
	private HelpAccount helpAccount;
	private List<UserPreference> preferences;
	private List<Warn> warns;
}
