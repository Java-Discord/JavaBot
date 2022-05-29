package net.javadiscord.javabot.systems.moderation.server_lock;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.ServerLockConfig;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;
import net.javadiscord.javabot.util.TimeUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
	private static final long GUILD_MEMBER_QUEUE_CLEAN_INTERVAL = 30L;

	private final Map<Long, Deque<Member>> guildMemberQueues;

	/**
	 * Contructor that initializes and handles the serverlock.
	 *
	 * @param jda The {@link JDA} instance.
	 */
	public ServerLock(JDA jda) {
		this.guildMemberQueues = new ConcurrentHashMap<>();
		Bot.asyncPool.scheduleWithFixedDelay(() -> {
			for (var guild : jda.getGuilds()) {
				var members = getMemberQueue(guild);
				while (members.size() > GUILD_MEMBER_QUEUE_CUTOFF) {
					members.removeLast();
				}
				if (isLocked(guild)) {
					log.info("Checking if it's safe to unlock server {}.", guild.getName());
					if (!members.isEmpty()) {
						checkForEndOfRaid(guild);
					} else {
						unlockServer(guild);
					}
				}
			}
		}, GUILD_MEMBER_QUEUE_CLEAN_INTERVAL, GUILD_MEMBER_QUEUE_CLEAN_INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 * The embed that is sent when a user tries to join while the server is locked.
	 *
	 * @param guild The current guild.
	 * @return The {@link MessageEmbed} object.
	 */
	public static MessageEmbed buildServerLockEmbed(Guild guild) {
		return new EmbedBuilder()
				.setAuthor(guild.getName() + " | Server locked \uD83D\uDD12", Constants.WEBSITE_LINK, guild.getIconUrl())
				.setColor(Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setDescription(String.format("""
						Unfortunately, this server is currently locked. Please try to join again later.
						Contact the server owner, %s, for more info.""", guild.getOwner().getAsMention())
				).build();
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		Guild g = event.getGuild();
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
	 *
	 * @param guild The guild to check.
	 * @return The collection of members who we think could be raiding the server.
	 */
	private Collection<Member> getPotentialRaiders(Guild guild) {
		Deque<Member> recentJoins = new LinkedList<>(getMemberQueue(guild));
		if (recentJoins.isEmpty()) return new HashSet<>();
		var config = Bot.config.get(guild).getServerLock();
		final var accountCreationCutoff = OffsetDateTime.now().minusDays(config.getMinimumAccountAgeInDays());
		final var memberJoinCutoff = OffsetDateTime.now().minusMinutes(10);

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
			var delta = Math.abs(Duration.between(previousJoin.getTimeJoined(), member.getTimeJoined()).toMillis() / 1000.0f);
			if (delta < config.getMinimumSecondsBetweenJoins()) {
				potentialRaiders.add(previousJoin);
				potentialRaiders.add(member);
			}
			boolean accountCreatedRecently = member.getTimeCreated().isAfter(accountCreationCutoff);
			boolean joinedRecently = member.getTimeJoined().isAfter(memberJoinCutoff);
			boolean joinedRapidlyAfterOther = delta < config.getMinimumSecondsBetweenJoins();
			// Check if this user has joined Discord recently.
			if (accountCreatedRecently && joinedRecently && joinedRapidlyAfterOther) {
				potentialRaiders.add(member);
			}
			previousJoin = member;
		}
		return potentialRaiders;
	}

	/**
	 * Checks to see if we should lock the guild by analyzing the pattern of
	 * users who recently joined the guild.
	 *
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
	 *
	 * @param guild The guild to check.
	 */
	private void checkForEndOfRaid(Guild guild) {
		var config = Bot.config.get(guild).getServerLock();
		if (!config.isLocked()) return;
		var potentialRaiders = getPotentialRaiders(guild);
		log.info("Found {} potential raiders while checking for end of raid.", potentialRaiders.size());
		if (potentialRaiders.size() < config.getLockThreshold()) {
			unlockServer(guild);
		}
	}

	/**
	 * Rejects a user's joining of the server during a raid attempt by sending
	 * them a message about it and kicking them immediately.
	 *
	 * @param event The user who joined.
	 */
	private void rejectUserDuringRaid(GuildMemberJoinEvent event) {
		event.getUser().openPrivateChannel().queue(c -> {
			c.sendMessage("https://discord.gg/java").setEmbeds(buildServerLockEmbed(event.getGuild())).queue(msg -> {
				event.getMember().kick().queue();
			});
		});
		String diff = new TimeUtils().formatDurationToNow(event.getMember().getTimeCreated());
		new GuildNotificationService(event.getGuild()).sendLogChannelNotification("**%s** (%s old) tried to join this server.", event.getMember().getUser().getAsTag(), diff);
	}

	/**
	 * Locks a server when a raid has been detected.
	 *
	 * @param guild            The guild to lock.
	 * @param potentialRaiders The list of members that we think are starting
	 *                         the raid.
	 */
	public void lockServer(Guild guild, Collection<Member> potentialRaiders) {
		for (var member : potentialRaiders) {
			member.getUser().openPrivateChannel().queue(c -> {
				c.sendMessage("https://discord.gg/java").setEmbeds(buildServerLockEmbed(guild)).queue(msg -> {
					member.kick().queue(
							success -> {},
							error -> new GuildNotificationService(guild).sendLogChannelNotification("Could not kick member %s%n> `%s`", member.getUser().getAsTag(), error.getMessage()));
				});
			});
		}

		String membersString = potentialRaiders.stream()
				.sorted(Comparator.comparing(Member::getTimeJoined).reversed())
				.map(m -> String.format(
						"- **%s** joined at `%s`, account is `%s` old.",
						m.getUser().getAsTag(),
						m.getTimeJoined().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSSS")),
						TimeUtils.formatDuration(Duration.between(m.getTimeCreated(), OffsetDateTime.now()))
				))
				.collect(Collectors.joining("\n"));

		var config = Bot.config.get(guild);
		config.getServerLock().setLocked("true");
		Bot.config.get(guild).flush();
		new GuildNotificationService(guild).sendLogChannelNotification("""
						**Server Locked** %s
						The automated locking system has detected that the following %d users may be part of a raid:
						%s
						""",
				config.getModeration().getStaffRole().getAsMention(),
				potentialRaiders.size(),
				membersString
		);
	}

	/**
	 * Unlocks the server after it has been deemed that we're no longer in a raid.
	 *
	 * @param guild The guild to unlock.
	 */
	public void unlockServer(Guild guild) {
		ServerLockConfig config = Bot.config.get(guild).getServerLock();
		config.setLocked("false");
		Bot.config.get(guild).flush();
		guildMemberQueues.clear();
		new GuildNotificationService(guild).sendLogChannelNotification("Server unlocked automatically.");
	}
}
