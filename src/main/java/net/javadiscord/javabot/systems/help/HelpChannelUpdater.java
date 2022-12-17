package net.javadiscord.javabot.systems.help;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Loops through all guilds and updates the corresponding help forum channel, closing inactive ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpChannelUpdater {
	private final JDA jda;
	private final BotConfig botConfig;

	/**
	 * Updates all help channels.
	 */
	@Scheduled(cron = "0 * * * * *") // Hourly
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			log.info("Updating help channels in {}", guild.getName());
			GuildConfig config = botConfig.get(guild);
			ForumChannel forum = config.getHelpForumConfig().getHelpForumChannel();
			if (forum != null) {
				for (ThreadChannel post : forum.getThreadChannels()) {
					if (post.isArchived() || post.isLocked()) continue;
					checkForumPost(post, config.getHelpConfig());
				}
			}
		}
	}

	private void checkForumPost(@NotNull ThreadChannel post, HelpConfig config) {
		post.retrieveMessageById(post.getLatestMessageId()).queue(latest -> {
			if (latest.getTimeCreated().plusMinutes(300).isBefore(OffsetDateTime.now())) {
				post.sendMessage(config.getDormantChannelMessage()).queue(s ->
						post.getManager().setLocked(true).setArchived(true).queue());
			}
		});
	}
}
