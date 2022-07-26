package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;
import net.javadiscord.javabot.systems.notification.UserNotificationService;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * This service provides methods for performing moderation actions, like banning
 * or warning users.
 */
@Slf4j
public class ModerationService {
	private static final int BAN_DELETE_DAYS = 7;

	private final ModerationConfig moderationConfig;

	/**
	 * Constructs the service.
	 *
	 * @param config The {@link GuildConfig} to use.
	 */
	public ModerationService(@NotNull GuildConfig config) {
		this.moderationConfig = config.getModerationConfig();
	}

	/**
	 * Constructs the service using information obtained from an interaction.
	 *
	 * @param interaction The interaction to use.
	 */
	public ModerationService(@NotNull Interaction interaction) {
		this(
				Bot.getConfig().get(interaction.getGuild())
		);
	}

	/**
	 * Issues a warning for the given user.
	 *
	 * @param user   The user to warn.
	 * @param severity The severity of the warning.
	 * @param reason   The reason for this warning.
	 * @param warnedBy The member who issued the warning.
	 * @param channel  The channel in which the warning was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 */
	public void warn(User user, WarnSeverity severity, String reason, Member warnedBy, MessageChannel channel, boolean quiet) {
		DbHelper.doDaoAction(WarnRepository::new, dao -> {
			dao.insert(new Warn(user.getIdLong(), warnedBy.getIdLong(), severity, reason));
			int totalSeverity = dao.getTotalSeverityWeight(user.getIdLong(), LocalDateTime.now().minusDays(moderationConfig.getWarnTimeoutDays()));
			MessageEmbed warnEmbed = buildWarnEmbed(user, warnedBy, severity, totalSeverity, reason);
			new UserNotificationService(user).sendDirectMessageNotification(warnEmbed);
			new GuildNotificationService(moderationConfig.getGuild()).sendLogChannelNotification(warnEmbed);
			if (!quiet && channel.getIdLong() != moderationConfig.getLogChannelId()) {
				channel.sendMessageEmbeds(warnEmbed).queue();
			}
			if (totalSeverity > moderationConfig.getMaxWarnSeverity()) {
				ban(user, "Too many warns", warnedBy, channel, quiet);
			}
		});
	}

	/**
	 * Clears warns from the given user by discarding all warns.
	 *
	 * @param user      The user to clear warns from.
	 * @param clearedBy The user who cleared the warns.
	 */
	public void discardAllWarns(User user, User clearedBy) {
		DbHelper.doDaoAction(WarnRepository::new, dao -> {
			dao.discardAll(user.getIdLong());
			MessageEmbed embed = buildClearWarnsEmbed(user, clearedBy);
			new UserNotificationService(user).sendDirectMessageNotification(embed);
			new GuildNotificationService(moderationConfig.getGuild()).sendLogChannelNotification(embed);
		});
	}

	/**
	 * Clears a warn by discarding the Warn with the corresponding id.
	 *
	 * @param id        The id of the warn to discard.
	 * @param clearedBy The user who cleared the warn.
	 * @return Whether the Warn was discarded or not.
	 */
	public boolean discardWarnById(long id, User clearedBy) {
		try (Connection con = Bot.getDataSource().getConnection()) {
			WarnRepository repo = new WarnRepository(con);
			Optional<Warn> warnOptional = repo.findById(id);
			if (warnOptional.isPresent()) {
				Warn warn = warnOptional.get();
				repo.discardById(warn.getId());
				new GuildNotificationService(moderationConfig.getGuild())
						.sendLogChannelNotification(buildClearWarnsByIdEmbed(warn, clearedBy));
				return true;
			}
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
		return false;
	}

	/**
	 * Gets all warns based on the user id.
	 *
	 * @param userId The user's id.
	 * @return A {@link List} with all warns.
	 */
	public List<Warn> getWarns(long userId) {
		try (Connection con = Bot.getDataSource().getConnection()) {
			WarnRepository repo = new WarnRepository(con);
			LocalDateTime cutoff = LocalDateTime.now().minusDays(moderationConfig.getWarnTimeoutDays());
			return repo.getWarnsByUserId(userId, cutoff);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return List.of();
		}
	}

	/**
	 * Adds a Timeout to the member.
	 *
	 * @param member     The member to time out.
	 * @param reason     The reason for adding this Timeout.
	 * @param timedOutBy The member who is responsible for adding this Timeout.
	 * @param duration   How long the Timeout should last.
	 * @param channel    The channel in which the Timeout was issued.
	 * @param quiet      If true, don't send a message in the channel.
	 */
	public void timeout(@Nonnull Member member, @Nonnull String reason, @Nonnull Member timedOutBy, @Nonnull Duration duration, @Nonnull MessageChannel channel, boolean quiet) {
		MessageEmbed timeoutEmbed = buildTimeoutEmbed(member, timedOutBy, reason, duration);
		member.getGuild().timeoutFor(member, duration).queue(s -> {
			new UserNotificationService(member.getUser()).sendDirectMessageNotification(timeoutEmbed);
			new GuildNotificationService(member.getGuild()).sendLogChannelNotification(timeoutEmbed);
			if (!quiet) channel.sendMessageEmbeds(timeoutEmbed).queue();
		}, ExceptionLogger::capture);
	}

	/**
	 * Removes a Timeout from a member.
	 *
	 * @param member    The member whose Timeout should be removed.
	 * @param reason    The reason for removing this Timeout.
	 * @param removedBy The member who is responsible for removing this Timeout.
	 * @param channel   The channel in which the Removal was issued.
	 * @param quiet     If true, don't send a message in the channel.
	 */
	public void removeTimeout(Member member, String reason, Member removedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed removeTimeoutEmbed = buildTimeoutRemovedEmbed(member, removedBy, reason);
		removedBy.getGuild().removeTimeout(member).queue(s -> {
			new UserNotificationService(member.getUser()).sendDirectMessageNotification(removeTimeoutEmbed);
			new GuildNotificationService(member.getGuild()).sendLogChannelNotification(removeTimeoutEmbed);
			if (!quiet) channel.sendMessageEmbeds(removeTimeoutEmbed).queue();
		}, ExceptionLogger::capture);
	}

	/**
	 * Bans a user.
	 *
	 * @param user     The user to ban.
	 * @param reason   The reason for banning the member.
	 * @param bannedBy The member who is responsible for banning this member.
	 * @param channel  The channel in which the ban was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 */
	public void ban(User user, String reason, Member bannedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed banEmbed = buildBanEmbed(user, bannedBy, reason);
		bannedBy.getGuild().ban(user, BAN_DELETE_DAYS, reason).queue(s -> {
			new UserNotificationService(user).sendDirectMessageNotification(banEmbed);
			new GuildNotificationService(bannedBy.getGuild()).sendLogChannelNotification(banEmbed);
			if (!quiet) channel.sendMessageEmbeds(banEmbed).queue();
		}, ExceptionLogger::capture);
	}

	/**
	 * Unbans a user.
	 *
	 * @param userId   The user's id.
	 * @param reason The reason for unbanning this user.
	 * @param bannedBy The member who is responsible for unbanning this member.
	 * @param channel  The channel in which the unban was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 * @return Whether the member is banned or not.
	 */
	public boolean unban(long userId, String reason, Member bannedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed unbanEmbed = this.buildUnbanEmbed(userId, reason, bannedBy);
		boolean isBanned = isBanned(bannedBy.getGuild(), userId);
		if (isBanned) {
			bannedBy.getGuild().unban(User.fromId(userId)).queue(s -> {
				moderationConfig.getLogChannel().sendMessageEmbeds(unbanEmbed).queue();
				if (!quiet) channel.sendMessageEmbeds(unbanEmbed).queue();
			}, ExceptionLogger::capture);
		}
		return isBanned;
	}

	private boolean isBanned(@NotNull Guild guild, long userId) {
		return guild.retrieveBanList().complete()
				.stream().map(Guild.Ban::getUser)
				.map(User::getIdLong).toList().contains(userId);
	}

	/**
	 * Kicks a member.
	 *
	 * @param user   The user to kick.
	 * @param reason   The reason for kicking the member.
	 * @param kickedBy The member who is responsible for kicking this member.
	 * @param channel  The channel in which the kick was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 */
	public void kick(User user, String reason, Member kickedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed kickEmbed = buildKickEmbed(user, kickedBy, reason);
		kickedBy.getGuild().kick(user).queue(s -> {
			new UserNotificationService(user).sendDirectMessageNotification(kickEmbed);
			new GuildNotificationService(kickedBy.getGuild()).sendLogChannelNotification(kickEmbed);
			if (!quiet) channel.sendMessageEmbeds(kickEmbed).queue();
		}, ExceptionLogger::capture);
	}

	private @NotNull EmbedBuilder buildModerationEmbed(@NotNull User user, @NotNull Member moderator, String reason) {
		return new EmbedBuilder()
				.setAuthor(moderator.getUser().getAsTag(), null, moderator.getEffectiveAvatarUrl())
				.addField("Member", user.getAsMention(), true)
				.addField("Moderator", moderator.getAsMention(), true)
				.addField("Reason", reason, true)
				.setTimestamp(Instant.now())
				.setFooter(user.getAsTag(), user.getEffectiveAvatarUrl());
	}

	private @NotNull MessageEmbed buildBanEmbed(User user, Member bannedBy, String reason) {
		return buildModerationEmbed(user, bannedBy, reason)
				.setTitle("Ban")
				.setColor(Responses.Type.ERROR.getColor())
				.build();
	}

	private @NotNull MessageEmbed buildKickEmbed(User user, Member kickedBy, String reason) {
		return buildModerationEmbed(user, kickedBy, reason)
				.setTitle("Kick")
				.setColor(Responses.Type.ERROR.getColor())
				.build();
	}

	private @NotNull MessageEmbed buildUnbanEmbed(long userId, String reason, @NotNull Member unbannedBy) {
		return new EmbedBuilder()
				.setAuthor(unbannedBy.getUser().getAsTag(), null, unbannedBy.getEffectiveAvatarUrl())
				.setTitle("Ban Revoked")
				.setColor(Responses.Type.ERROR.getColor())
				.addField("Moderator", unbannedBy.getAsMention(), true)
				.addField("Reason", reason, true)
				.addField("User Id", MarkdownUtil.codeblock(String.valueOf(userId)), false)
				.setTimestamp(Instant.now())
				.build();
	}

	private @NotNull MessageEmbed buildWarnEmbed(User user, Member warnedBy, @NotNull WarnSeverity severity, int totalSeverity, String reason) {
		return buildModerationEmbed(user, warnedBy, reason)
				.setTitle(String.format("Warn Added (%d/%d)", totalSeverity, moderationConfig.getMaxWarnSeverity()))
				.setColor(Responses.Type.WARN.getColor())
				.addField("Severity", String.format("`%s (%s)`", severity.name(), severity.getWeight()), true)
				.build();
	}

	private @NotNull MessageEmbed buildClearWarnsEmbed(@NotNull User user, @NotNull User clearedBy) {
		return new EmbedBuilder()
				.setAuthor(clearedBy.getAsTag(), null, clearedBy.getEffectiveAvatarUrl())
				.setTitle("Warns Cleared")
				.setColor(Responses.Type.WARN.getColor())
				.setDescription("All warns have been cleared from " + user.getAsMention() + "'s record.")
				.setTimestamp(Instant.now())
				.setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
				.build();
	}

	private @NotNull MessageEmbed buildClearWarnsByIdEmbed(@NotNull Warn w, @NotNull User clearedBy) {
		return new EmbedBuilder()
				.setAuthor(clearedBy.getAsTag(), null, clearedBy.getEffectiveAvatarUrl())
				.setTitle("Warn Cleared")
				.setColor(Responses.Type.WARN.getColor())
				.setDescription(String.format("""
								Cleared the following warn from <@%s>'s record:

								`%s` <t:%s>
								Warned by: <@%s>
								Severity: `%s (%s)`
								Reason: %s""",
						w.getUserId(), w.getId(), w.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond(),
						w.getWarnedBy(), w.getSeverity(), w.getSeverityWeight(), w.getReason()))
				.setTimestamp(Instant.now())
				.build();
	}

	private @NotNull MessageEmbed buildTimeoutEmbed(@NotNull Member member, Member timedOutBy, String reason, Duration duration) {
		return buildModerationEmbed(member.getUser(), timedOutBy, reason)
				.setTitle("Timeout")
				.setColor(Responses.Type.ERROR.getColor())
				.addField("Until", String.format("<t:%d>", Instant.now().plus(duration).getEpochSecond()), true)
				.build();
	}

	private @NotNull MessageEmbed buildTimeoutRemovedEmbed(@NotNull Member member, Member timedOutBy, String reason) {
		return buildModerationEmbed(member.getUser(), timedOutBy, reason)
				.setTitle("Timeout Removed")
				.setColor(Responses.Type.SUCCESS.getColor())
				.build();
	}
}