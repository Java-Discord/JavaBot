package net.javadiscord.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;

import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpManager;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for the {@link GuildMemberRemoveEvent}.
 */
@Slf4j
@RequiredArgsConstructor
public class UserLeaveListener extends ListenerAdapter {
	private final DbActions dbActions;
	private final BotConfig botConfig;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
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
		HelpConfig config = botConfig.get(guild).getHelpConfig();
		ForumChannel forum = config.getHelpForumChannel();
		if (forum != null) {
			for (ThreadChannel post : forum.getThreadChannels()) {
				if (post.isArchived() || post.isLocked()) continue;
				if (post.getOwnerIdLong() == user.getIdLong()) {
					HelpManager manager = new HelpManager(post, dbActions, botConfig, helpAccountRepository, helpTransactionRepository);
					manager.close(UserSnowflake.fromId(guild.getSelfMember().getIdLong()), "User left the server");
				}
			}
		} else {
			log.warn("Could not find forum channel for guild {}", guild.getName());
		}
	}
}
