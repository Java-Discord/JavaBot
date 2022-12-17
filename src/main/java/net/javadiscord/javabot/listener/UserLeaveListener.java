package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;

import lombok.RequiredArgsConstructor;

/**
 * Listens for the {@link GuildMemberRemoveEvent}.
 */
@RequiredArgsConstructor
public class UserLeaveListener extends ListenerAdapter {
	private final BotConfig botConfig;

	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		if (event.getUser().isBot() || event.getUser().isSystem()) return;
		if (!botConfig.get(event.getGuild()).getServerLockConfig().isLocked()) {
			unreserveAllChannels(event.getUser(), event.getGuild());
		}
	}

	/**
	 * Unreserves any help channels that a leaving user may have reserved.
	 *
	 * @param user  The user who is leaving.
	 * @param guild The guild they're leaving.
	 */
	private void unreserveAllChannels(User user, Guild guild) {
		// TODO: Implement Forum
	}
}
