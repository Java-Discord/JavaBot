package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.util.ExceptionLogger;

import java.sql.SQLException;

/**
 * Listens for the {@link GuildMemberRemoveEvent}.
 */
public class UserLeaveListener extends ListenerAdapter {
	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		if (event.getUser().isBot() || event.getUser().isSystem()) return;
		if (!Bot.config.get(event.getGuild()).getServerLockConfig().isLocked()) {
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
		try {
			HelpChannelManager manager = new HelpChannelManager(Bot.config.get(guild).getHelpConfig());
			manager.unreserveAllOwnedChannels(user);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			TextChannel logChannel = Bot.config.get(guild).getModerationConfig().getLogChannel();
			logChannel.sendMessage("Database error while unreserving channels for a user who left: " + e.getMessage()).queue();
		}
	}
}
