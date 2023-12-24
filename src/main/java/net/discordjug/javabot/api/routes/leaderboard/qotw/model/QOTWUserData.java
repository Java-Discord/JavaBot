package net.discordjug.javabot.api.routes.leaderboard.qotw.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.discordjug.javabot.api.routes.data.UserData;
import net.discordjug.javabot.systems.qotw.model.QOTWAccount;
import net.dv8tion.jda.api.entities.User;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API-Data class which contains all necessary information about a single users'
 * QOTW-Account.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QOTWUserData extends UserData {
	private QOTWAccount account;
	private int rank;

	/**
	 * Creates a new {@link QOTWUserData} instance.
	 *
	 * @param account The {@link QOTWAccount} to use.
	 * @param user A nullable {@link User}.
	 * @param rank The position of the user in the QOTW leaderboard
	 * @return The {@link QOTWUserData}.
	 */
	public static @NotNull QOTWUserData of(@NotNull QOTWAccount account, @Nullable User user, int rank) {
		QOTWUserData data = new QOTWUserData();
		data.setUserId(account.getUserId());
		if (user != null) {
			data.setUserName(user.getName());
			data.setDiscriminator(user.getDiscriminator());
			data.setEffectiveAvatarUrl(user.getEffectiveAvatarUrl());
		}
		data.setRank(rank);
		data.setAccount(account);
		return data;
	}
}
