package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public interface CommandModerationPermissions {
	default void setModerationSlashCommandData(@NotNull SlashCommandData data) {
		setSlashCommandData(data.setGuildOnly(true)
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
		);
	}

	void setSlashCommandData(SlashCommandData data);
}
