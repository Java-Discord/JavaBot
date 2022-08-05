package net.javadiscord.javabot.api.routes.leaderboard.help_experience.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.api.routes.data.UserData;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API-Data class which contains all necessary information about a single users'
 * Help-Account.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ExperienceUserData extends UserData {
	private HelpAccount account;

	/**
	 * Creates a new {@link ExperienceUserData} instance.
	 *
	 * @param account The {@link HelpAccount} to use.
	 * @param user A nullable {@link User}.
	 * @return The {@link ExperienceUserData}.
	 */
	public static @NotNull ExperienceUserData of(@NotNull HelpAccount account, @Nullable User user) {
		ExperienceUserData data = new ExperienceUserData();
		data.setUserId(account.getUserId());
		if (user != null) {
			data.setUserName(user.getName());
			data.setDiscriminator(user.getDiscriminator());
			data.setEffectiveAvatarUrl(user.getEffectiveAvatarUrl());
		}
		data.setAccount(account);
		return data;
	}
}
