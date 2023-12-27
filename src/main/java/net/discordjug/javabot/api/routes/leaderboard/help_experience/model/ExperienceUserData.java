package net.discordjug.javabot.api.routes.leaderboard.help_experience.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.discordjug.javabot.api.routes.data.UserData;
import net.discordjug.javabot.systems.help.model.HelpAccount;
import net.dv8tion.jda.api.entities.User;

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
	private int rank;

	/**
	 * Creates a new {@link ExperienceUserData} instance.
	 *
	 * @param account The {@link HelpAccount} to use.
	 * @param user A nullable {@link User}.
	 * @param rank The position of the user in the help leaderboard.
	 * @return The {@link ExperienceUserData}.
	 */
	public static @NotNull ExperienceUserData of(@NotNull HelpAccount account, @Nullable User user, int rank) {
		ExperienceUserData data = new ExperienceUserData();
		data.setUserId(account.getUserId());
		if (user != null) {
			data.setUserName(user.getName());
			data.setDiscriminator(user.getDiscriminator());
			data.setEffectiveAvatarUrl(user.getEffectiveAvatarUrl());
		}
		data.setAccount(account);
		data.setRank(rank);
		return data;
	}
}
