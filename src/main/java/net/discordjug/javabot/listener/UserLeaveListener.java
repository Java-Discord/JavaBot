package net.discordjug.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.guild.HelpConfig;
import net.discordjug.javabot.data.h2db.DbActions;
import net.discordjug.javabot.systems.help.HelpManager;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

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
	private final UserPreferenceService userPreferenceService;

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		User user = event.getUser();
		if (user.isBot() || user.isSystem()) return;
		Guild guild = event.getGuild();
		GuildConfig guildConfig = botConfig.get(guild);
		if (!guildConfig.getServerLockConfig().isLocked()) {
			unreserveAllHelpChannels(user, guild);
			closeAllPostsOfUser(guildConfig.getModerationConfig().getJobChannel(), user, guild);
			closeAllPostsOfUser(guildConfig.getModerationConfig().getProjectChannel(), user, guild);
		}
	}

	/**
	 * Unreserves any help channels that a leaving user may have reserved.
	 *
	 * @param user  The user who is leaving.
	 * @param guild The guild they're leaving.
	 */
	private void unreserveAllHelpChannels(User user, Guild guild) {
		HelpConfig config = botConfig.get(guild).getHelpConfig();
		ForumChannel forum = config.getHelpForumChannel();
		executeForAllOpenPostsOfUser(guild, user, forum, this::unreserveHelpChannel);
	}

	private void closeAllPostsOfUser(ForumChannel channel, User user, Guild guild) {
		executeForAllOpenPostsOfUser(guild, user, channel, post ->
			post
				.sendMessage("This post has been unreserved due to the original poster leaving the server.")
				.flatMap(msg ->
					post
						.getManager()
						.setArchived(true)
						.setLocked(true))
				.queue()
		);
	}

	private void executeForAllOpenPostsOfUser(Guild guild, User user, ForumChannel forum, Consumer<ThreadChannel> toExecute) {
		if (forum != null) {
			for (ThreadChannel post : forum.getThreadChannels()) {
				if (post.isArchived() || post.isLocked()) continue;
				if (post.getOwnerIdLong() == user.getIdLong()) {
					toExecute.accept(post);
				}
			}
		} else {
			log.warn("Could not find forum channel for guild {}", guild.getName());
		}
	}

	private void unreserveHelpChannel(ThreadChannel post) {
		HelpManager manager = new HelpManager(post, dbActions, botConfig, helpAccountRepository, helpTransactionRepository, userPreferenceService);
		manager.close(UserSnowflake.fromId(post.getGuild().getSelfMember().getIdLong()), "User left the server");
	}
}
