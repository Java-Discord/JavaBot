package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

/**
 * Interface which adds `setModerationSlashCommandData`. This method simply uses the provided {@link SlashCommandData}
 * and forces it to be (1.) guild only and (2.) only enabled for users with the {@link Permission#MODERATE_MEMBERS}
 * permission.
 */
public interface CommandModerationPermissions {
	/**
	 * Takes the given {@link SlashCommandData} and forces it to be
	 * guild only enabled for users with the {@link Permission#MODERATE_MEMBERS}
	 * permission.
	 *
	 * @param data The {@link SlashCommandData}.
	 */
	default void setModerationSlashCommandData(@NotNull SlashCommandData data) {
		setCommandData(data.setGuildOnly(true)
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
		);
	}

	void setCommandData(SlashCommandData data);
}
