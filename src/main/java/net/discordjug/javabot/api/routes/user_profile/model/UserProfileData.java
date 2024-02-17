package net.discordjug.javabot.api.routes.user_profile.model;

import lombok.Data;
import net.discordjug.javabot.systems.moderation.warn.model.Warn;
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;

import java.time.OffsetDateTime;
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
	private List<Warn> warns;
	private boolean isGuildMember;
	private OffsetDateTime guildJoinedDateTime;

}
