package net.discordjug.javabot.systems.help;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.HelpConfig;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.discordjug.javabot.systems.user_preferences.model.Preference;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Loops through all guilds and updates the corresponding help forum channel, closing inactive ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpForumUpdater {
	private final JDA jda;
	private final BotConfig botConfig;
	private final UserPreferenceService preferenceService;
	private final HelpExperienceService experienceService;

	/**
	 * Updates all help channels.
	 */
	@Scheduled(cron = "0 */10 * * * *") // Run every 10 minutes
	public void execute() {
		for (Guild guild : jda.getGuilds()) {
			log.debug("Checking for inactive forum posts in {}", guild.getName());
			HelpConfig config = botConfig.get(guild).getHelpConfig();
			ForumChannel forum = config.getHelpForumChannel();
			if (forum != null) {
				for (ThreadChannel post : forum.getThreadChannels()) {
					if (post.isArchived() || post.isLocked()) continue;
					checkForumPost(post, config);
				}
			} else {
				log.warn("Could not find forum channel for guild {}", guild.getName());
			}
		}
	}

	private void checkForumPost(@NotNull ThreadChannel post, HelpConfig config) {
		post.getHistory().retrievePast(1).queue(messages -> {
			if (messages.isEmpty()) {
				log.error("Could not find messages in forum thread {}", post.getId());
				return;
			}
			// Simply get the first one, as we only requested a singular message
			Message latest = messages.get(0);
			long minutesAgo = (Instant.now().getEpochSecond() - latest.getTimeCreated().toEpochSecond()) / 60;
			boolean isThankMessage = isThanksMessage(latest);
			if (minutesAgo > config.getInactivityTimeoutMinutes() || minutesAgo > config.getRemoveThanksTimeoutMinutes() && isThankMessage) {
				if (isThankMessage) {
					latest.delete().queue();
				}
				post.sendMessage(config.getDormantChannelMessageTemplate().formatted(config.getInactivityTimeoutMinutes())).queue(s -> {
					post.getManager().setArchived(true).queue();
					sendDMDormantInfoIfEnabled(post, config);
					experienceService.addMessageBasedHelpXP(post, false);
					log.info("Archived forum thread '{}' (by {}) for inactivity (last message sent {} minutes ago)",
							post.getName(), post.getOwnerId(), minutesAgo);
					
				});
			}
		}, e -> log.error("Could not find latest message in forum thread {}:", post.getId(), e));
	}

	private void sendDMDormantInfoIfEnabled(ThreadChannel post, HelpConfig config) {
		if(Boolean.parseBoolean(preferenceService.getOrCreate(post.getOwnerIdLong(), Preference.PRIVATE_DORMANT_NOTIFICATIONS).getState())) {
			post
				.getJDA()
				.openPrivateChannelById(post.getOwnerIdLong())
				.flatMap(c -> c.sendMessageEmbeds(createDMDormantInfo(post, config)))
				.queue();
		}
	}

	private MessageEmbed createDMDormantInfo(ThreadChannel post, HelpConfig config) {
		return new EmbedBuilder()
				.setTitle("Post marked as dormant")
				.setDescription(
						config
						.getDormantChannelPrivateMessageTemplate()
						.formatted(
								post.getAsMention(),
								post.getParentChannel().getAsMention(),
								config.getInactivityTimeoutMinutes(),
								post.getJumpUrl()))
				.build()
				;
	}

	private boolean isThanksMessage(@NotNull Message m) {
		return m.getAuthor().isBot() && !m.getButtons().isEmpty() &&
				m.getButtons().stream().allMatch(b -> b.getId() != null && b.getId().contains(HelpManager.HELP_THANKS_IDENTIFIER));
	}
}
