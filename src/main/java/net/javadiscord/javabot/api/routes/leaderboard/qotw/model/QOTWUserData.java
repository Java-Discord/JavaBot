package net.javadiscord.javabot.api.routes.leaderboard.qotw.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.User;
import net.javadiscord.javabot.api.routes.data.UserData;
import net.javadiscord.javabot.systems.qotw.model.QOTWAccount;
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

	/**
	 * Creates a new {@link QOTWUserData} instance.
	 *
	 * @param account The {@link QOTWAccount} to use.
	 * @param user A nullable {@link User}.
	 * @return The {@link QOTWUserData}.
	 */
	public static @NotNull QOTWUserData of(@NotNull QOTWAccount account, @Nullable User user) {
		QOTWUserData data = new QOTWUserData();
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
