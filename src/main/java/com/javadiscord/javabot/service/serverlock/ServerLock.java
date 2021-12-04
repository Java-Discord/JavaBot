package com.javadiscord.javabot.service.serverlock;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.Constants;
import com.javadiscord.javabot.utils.Misc;
import com.javadiscord.javabot.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Server lock functionality that automatically locks the server if a raid is detected.
 */
@Slf4j
public class ServerLock extends ListenerAdapter {
	/**
	 * Every time we clean the guild member queues, we leave this many members
	 * in the queue, at least.
	 */
	private static final int GUILD_MEMBER_QUEUE_CUTOFF = 20;

	/**
	 * How often to clean the guild member queues, in seconds.
	 */
	private static final long GUILD_MEMBER_QUEUE_CLEAN_INTERVAL = 60L;

	private final Map<Long, Deque<Member>> guildMemberQueues;

	public ServerLock() {
		this.guildMemberQueues = new ConcurrentHashMap<>();
		Bot.asyncPool.scheduleWithFixedDelay(() -> {
			for (var entry : guildMemberQueues.entrySet()) {
				var members = entry.getValue();
				while (members.size() > GUILD_MEMBER_QUEUE_CUTOFF) {
					members.removeLast();
				}
				if (!members.isEmpty()) {
					checkForEndOfRaid(members.peek().getGuild());
				}
			}
		}, GUILD_MEMBER_QUEUE_CLEAN_INTERVAL, GUILD_MEMBER_QUEUE_CLEAN_INTERVAL, TimeUnit.SECONDS);
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		var g = event.getGuild();
		getMemberQueue(g).addFirst(event.getMember());
		if (isLocked(g)) {
			rejectUserDuringRaid(event);
		} else {
			checkForRaid(g);
		}
	}

	private boolean isLocked(Guild guild) {
		return Bot.config.get(guild).getServerLock().isLocked();
	}

	private Deque<Member> getMemberQueue(Guild guild) {
		return guildMemberQueues.computeIfAbsent(guild.getIdLong(), n -> new ConcurrentLinkedDeque<>());
	}

	/**
	 * We use the following criteria to build a list of recently joined members
	 * who might be potential raiders.
	 * <ul>
	 *     <li>More than a threshold number of users whose accounts are younger
	 *     than a set age have joined.</li>
	 *     <li>More than a threshold number of users have joined within a small
	 *     window of time.</li>
	 * </ul>
	 * @param guild The guild to check.
	 * @return The collection of members who we think could be raiding the server.
	 */
	private Collection<Member> getPotentialRaiders(Guild guild) {
		Deque<Member> recentJoins = new LinkedList<>(getMemberQueue(guild));
		if (recentJoins.isEmpty()) return new HashSet<>();
		var config = Bot.config.get(guild).getServerLock();
		final var accountCreationCutoff = OffsetDateTime.now().minusDays(config.getMinimumAccountAgeInDays());

		Set<Member> potentialRaiders = new HashSet<>();
		var it = recentJoins.iterator();
		Member previousJoin = it.next();
		// Do account join check for first member, so that we can do the rest during the time delta check loop.
		if (previousJoin.getTimeCreated().isAfter(accountCreationCutoff)) {
			potentialRaiders.add(previousJoin);
		}
		while (it.hasNext()) {
			var member = it.next();
			// Check the time between when the previous member joined, and when this one joined.
			var delta = Duration.between(previousJoin.getTimeJoined(), member.getTimeJoined()).toMillis() / 1000.0f;
			if (delta < config.getMinimumSecondsBetweenJoins()) {
				potentialRaiders.add(previousJoin);
				potentialRaiders.add(member);
			}
			// Check if this user has joined Discord recently.
			if (member.getTimeCreated().isAfter(accountCreationCutoff)) {
				potentialRaiders.add(member);
			}
			previousJoin = member;
		}
		return potentialRaiders;
	}

	/**
	 * Checks to see if we should lock the guild by analyzing the pattern of
	 * users who recently joined the guild.
	 * @param guild The guild to check.
	 */
	private void checkForRaid(Guild guild) {
		var potentialRaiders = getPotentialRaiders(guild);
		var config = Bot.config.get(guild).getServerLock();
		if (potentialRaiders.size() >= config.getLockThreshold()) {
			lockServer(guild, potentialRaiders);
		}
	}

	/**
	 * Checks to see if we should unlock the guild.
	 * @param guild The guild to check.
	 */
	private void checkForEndOfRaid(Guild guild) {
		var potentialRaiders = getPotentialRaiders(guild);
		var config = Bot.config.get(guild).getServerLock();
		if (potentialRaiders.size() < config.getLockThreshold()) {
			unlockServer(guild);
		}
	}

	/**
	 * Rejects a user's joining of the server during a raid attempt by sending
	 * them a message about it and kicking them immediately.
	 * @param event The user who joined.
	 */
	private void rejectUserDuringRaid(GuildMemberJoinEvent event) {
		event.getUser().openPrivateChannel().queue(c -> {
			c.sendMessage("https://discord.gg/java")
					.setEmbeds(lockEmbed(event.getGuild())).queue();
			event.getMember().kick().queue();
		});
		String diff = new TimeUtils().formatDurationToNow(event.getMember().getTimeCreated());
		Misc.sendToLog(event.getGuild(), String.format("**%s** (%s old) tried to join this server.",
				event.getMember().getUser().getAsTag(), diff));
	}

	/**
	 * Locks a server when a raid has been detected.
	 * @param guild The guild to lock.
	 * @param potentialRaiders The list of members that we think are starting
	 *                         the raid.
	 */
	private void lockServer(Guild guild, Collection<Member> potentialRaiders) {
		for (var member : potentialRaiders) {
			member.getUser().openPrivateChannel().queue(c -> {
				c.sendMessage("https://discord.gg/java")
						.setEmbeds(lockEmbed(guild)).queue();
				try {
					member.kick().queue();
				} catch (Exception e) {
					Misc.sendToLog(guild, String.format("Could not kick member %s%n> `%s`", member.getUser().getAsTag(), e.getMessage()));
				}
			});
		}

		var config = Bot.config.get(guild).getServerLock();
		config.setLocked("true");
		Bot.config.get(guild).flush();
		Misc.sendToLog(guild, config.getLockMessageTemplate());
	}

	/**
	 * Unlocks the server after it has been deemed that we're no longer in a raid.
	 * @param guild The guild to unlock.
	 */
	private void unlockServer(Guild guild) {
		var config = Bot.config.get(guild).getServerLock();
		config.setLocked("false");
		Bot.config.get(guild).flush();
		Misc.sendToLog(guild, "Server unlocked automatically.");
	}

	/**
	 * The embed that is sent when a user tries to join while the server is locked.
	 * @param guild The current guild.
	 */
	public static MessageEmbed lockEmbed(Guild guild) {
		return new EmbedBuilder()
				.setAuthor(guild.getName() + " | Server locked \uD83D\uDD12", Constants.WEBSITE_LINK, guild.getIconUrl())
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setDescription(String.format("""
        Unfortunately, this server is currently locked. Please try to join again later.
        Contact the server owner, %s, for more info.""", guild.getOwner().getAsMention())
				).build();
	}
}
