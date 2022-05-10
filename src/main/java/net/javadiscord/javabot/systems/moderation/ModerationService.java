package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.Interaction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * This service provides methods for performing moderation actions, like banning
 * or warning users.
 */
@Slf4j
public class ModerationService {
	private static final int BAN_DELETE_DAYS = 7;

	private final JDA jda;
	private final ModerationConfig moderationConfig;
	private final SlashCommandConfig slashCommandConfig;
	private final String reasonFormat = "```\n%s\n```";

	/**
	 * Constructs the service.
	 *
	 * @param jda    The API to use to interact with various discord entities.
	 * @param config The {@link GuildConfig} to use.
	 */
	public ModerationService(JDA jda, GuildConfig config) {
		this.jda = jda;
		this.moderationConfig = config.getModeration();
		this.slashCommandConfig = config.getSlashCommand();
	}

	/**
	 * Constructs the service using information obtained from an interaction.
	 *
	 * @param interaction The interaction to use.
	 */
	public ModerationService(Interaction interaction) {
		this(
				interaction.getJDA(),
				Bot.config.get(interaction.getGuild())
		);
	}

	/**
	 * Issues a warning for the given user.
	 *
	 * @param member   The member to warn.
	 * @param severity The severity of the warning.
	 * @param reason   The reason for this warning.
	 * @param warnedBy The member who issued the warning.
	 * @param channel  The channel in which the warning was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 */
	public void warn(Member member, WarnSeverity severity, String reason, Member warnedBy, MessageChannel channel, boolean quiet) {
		DbHelper.doDbAction(con -> {
			var repo = new WarnRepository(con);
			var warn = repo.insert(new Warn(member.getIdLong(), warnedBy.getIdLong(), severity, reason));
			LocalDateTime cutoff = LocalDateTime.now().minusDays(moderationConfig.getWarnTimeoutDays());
			int totalSeverity = repo.getTotalSeverityWeight(member.getIdLong(), cutoff);
			MessageEmbed warnEmbed = this.buildWarnEmbed(member, warnedBy, warn, severity, totalSeverity, reason);
			member.getUser().openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(warnEmbed).queue(),
					e -> log.info("Could not send Warn Direct Message to User {}", member.getUser().getAsTag())
			);
			moderationConfig.getLogChannel().sendMessageEmbeds(warnEmbed).queue();
			if (!quiet && channel.getIdLong() != moderationConfig.getLogChannelId()) {
				channel.sendMessageEmbeds(warnEmbed).queue();
			}
			if (totalSeverity > moderationConfig.getMaxWarnSeverity()) {
				this.ban(member, "Too many warns", warnedBy, channel, quiet);
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
			MessageEmbed embed = this.buildClearWarnsEmbed(user, clearedBy);
			user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embed).queue());
			moderationConfig.getLogChannel().sendMessageEmbeds(embed).queue();
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
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new WarnRepository(con);
			if (repo.findById(id).isEmpty()) return false;
			Warn w = repo.findById(id).get();
			repo.discardById(w.getId());
			MessageEmbed embed = this.buildClearWarnsByIdEmbed(w, clearedBy);
			moderationConfig.getLogChannel().sendMessageEmbeds(embed).queue();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Gets all warns based on the user id.
	 *
	 * @param userId The user's id.
	 * @return A {@link List} with all warns.
	 */
	public List<Warn> getWarns(long userId) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new WarnRepository(con);
			LocalDateTime cutoff = LocalDateTime.now().minusDays(moderationConfig.getWarnTimeoutDays());
			return repo.getWarnsByUserId(userId, cutoff);
		} catch (SQLException e) {
			e.printStackTrace();
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
	 * @return Whether the moderator has the permission to time out this member or not.
	 */
	public boolean timeout(Member member, String reason, Member timedOutBy, Duration duration, MessageChannel channel, boolean quiet) {
		MessageEmbed timeoutEmbed = this.buildTimeoutEmbed(member, timedOutBy, reason, duration);
		boolean canTimeout = this.canTimeoutUser(member, timedOutBy);
		if (canTimeout) {
			member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(timeoutEmbed).queue(),
					e -> log.info("Could not send Timeout Direct Message to User {}", member.getUser().getAsTag())
			);
			timedOutBy.getGuild().timeoutFor(member, duration).queue();
			moderationConfig.getLogChannel().sendMessageEmbeds(timeoutEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(timeoutEmbed).queue();
		}
		return canTimeout;
	}

	/**
	 * Removes a Timeout from a member.
	 *
	 * @param member    The member whose Timeout should be removed.
	 * @param reason    The reason for removing this Timeout.
	 * @param removedBy The member who is responsible for removing this Timeout.
	 * @param channel   The channel in which the Removal was issued.
	 * @param quiet     If true, don't send a message in the channel.
	 * @return Whether the moderator has the permission to remove this Timeout or not.
	 */
	public boolean removeTimeout(Member member, String reason, Member removedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed removeTimeoutEmbed = this.buildTimeoutRemovedEmbed(member, removedBy, reason);
		boolean canTimeout = this.canTimeoutUser(member, removedBy);
		if (canTimeout) {
			member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(removeTimeoutEmbed).queue(),
					e -> log.info("Could not send Timeout Direct Message to User {}", member.getUser().getAsTag())
			);
			removedBy.getGuild().removeTimeout(member).queue();
			moderationConfig.getLogChannel().sendMessageEmbeds(removeTimeoutEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(removeTimeoutEmbed).queue();
		}
		return canTimeout;
	}

	/**
	 * Bans a member.
	 *
	 * @param member   The member to ban.
	 * @param reason   The reason for banning the member.
	 * @param bannedBy The member who is responsible for banning this member.
	 * @param channel  The channel in which the ban was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 * @return Whether the moderator has the permission to ban this member or not.
	 */
	public boolean ban(Member member, String reason, Member bannedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed banEmbed = this.buildBanEmbed(member, reason, bannedBy);
		boolean canBan = this.canBanUser(member, bannedBy);
		if (canBan) {
			member.getUser().openPrivateChannel().queue(
					c -> c.sendMessage(moderationConfig.getBanMessageText()).setEmbeds(banEmbed).queue(),
					e -> log.info("Could not send Ban Direct Message to User {}", member.getUser().getAsTag())
			);
			bannedBy.getGuild().ban(member, BAN_DELETE_DAYS, reason).queue();
			moderationConfig.getLogChannel().sendMessageEmbeds(banEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(banEmbed).queue();
		}
		return canBan;
	}

	/**
	 * Unbans a member.
	 *
	 * @param userId   The user's id.
	 * @param bannedBy The member who is responsible for unbanning this member.
	 * @param channel  The channel in which the unban was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 * @return Whether the member is banned or not.
	 */
	public boolean unban(long userId, Member bannedBy, MessageChannel channel, boolean quiet) {
		MessageEmbed unbanEmbed = this.buildUnbanEmbed(userId, bannedBy);
		boolean isBanned = this.isBanned(bannedBy.getGuild(), userId);
		if (isBanned) {
			bannedBy.getGuild().unban(User.fromId(userId)).queue();
			moderationConfig.getLogChannel().sendMessageEmbeds(unbanEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(unbanEmbed).queue();
		}
		return isBanned;
	}

	private boolean isBanned(Guild guild, long userId) {
		return guild.retrieveBanList().complete()
				.stream().map(Guild.Ban::getUser)
				.map(User::getIdLong).toList().contains(userId);
	}

	private boolean canBanUser(Member member, Member bannedBy) {
		var perms = bannedBy.getPermissions();
		if (!perms.isEmpty() && !perms.contains(Permission.BAN_MEMBERS)) return false;
		if (member.getRoles().isEmpty()) return true;
		return !bannedBy.getRoles().isEmpty() &&
				member.getRoles().get(0).getPosition() < bannedBy.getRoles().get(0).getPosition();
	}

	private boolean canKickUser(Member member, Member kickedBy) {
		var perms = kickedBy.getPermissions();
		if (!perms.isEmpty() && !perms.contains(Permission.KICK_MEMBERS)) return false;
		if (member.getRoles().isEmpty()) return true;
		return !kickedBy.getRoles().isEmpty() &&
				member.getRoles().get(0).getPosition() < kickedBy.getRoles().get(0).getPosition();
	}

	private boolean canTimeoutUser(Member member, Member timedOutBy) {
		var perms = timedOutBy.getPermissions();
		if (!perms.isEmpty() && !perms.contains(Permission.MODERATE_MEMBERS)) return false;
		if (member.getRoles().isEmpty()) return true;
		return !timedOutBy.getRoles().isEmpty() &&
				member.getRoles().get(0).getPosition() < timedOutBy.getRoles().get(0).getPosition();
	}

	/**
	 * Kicks a member.
	 *
	 * @param member   The member to kick.
	 * @param reason   The reason for kicking the member.
	 * @param kickedBy The member who is responsible for kicking this member.
	 * @param channel  The channel in which the kick was issued.
	 * @param quiet    If true, don't send a message in the channel.
	 * @return Whether the moderator has the permission to kick this member or not.
	 */
	public boolean kick(Member member, String reason, Member kickedBy, MessageChannel channel, boolean quiet) {
		var kickEmbed = this.buildKickEmbed(member, kickedBy, reason);
		boolean canKick = this.canKickUser(member, kickedBy);
		if (canKick) {
			member.getUser().openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(kickEmbed).queue(
					success -> {
					}, e -> log.info("Could not send Kick Direct Message to User {}", member.getUser().getAsTag())));
			member.getGuild().kick(member).queue();
			moderationConfig.getLogChannel().sendMessageEmbeds(kickEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(kickEmbed).queue();
		}
		return canKick;
	}

	private MessageEmbed buildWarnEmbed(Member member, Member warnedBy, Warn warn, WarnSeverity severity, int totalSeverity, String reason) {
		return new EmbedBuilder()
				.setAuthor(warnedBy.getUser().getAsTag(), null, warnedBy.getEffectiveAvatarUrl())
				.setTitle(String.format("Warn Added (%d/%d)", totalSeverity, moderationConfig.getMaxWarnSeverity()))
				.setColor(slashCommandConfig.getWarningColor())
				.addField("Member", member.getAsMention(), true)
				.addField("Warned by", warnedBy.getAsMention(), true)
				.addField("Severity", String.format("`%s (%s)`", severity.name(), severity.getWeight()), true)
				.addField("Warn Reason", String.format(reasonFormat, reason), false)
				.setTimestamp(warn.getCreatedAt().toInstant(ZoneOffset.UTC))
				.setFooter(member.getUser().getAsTag(), member.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildKickEmbed(Member member, Member kickedBy, String reason) {
		return new EmbedBuilder()
				.setAuthor(kickedBy.getUser().getAsTag(), null, kickedBy.getEffectiveAvatarUrl())
				.setTitle("Kick")
				.setColor(slashCommandConfig.getErrorColor())
				.addField("Member", member.getAsMention(), true)
				.addField("Kicked by", kickedBy.getAsMention(), true)
				.addField("Kick Reason", String.format(reasonFormat, reason), false)
				.setTimestamp(Instant.now())
				.setFooter(member.getUser().getAsTag(), member.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildTimeoutEmbed(Member member, Member timedOutBy, String reason, Duration duration) {
		return new EmbedBuilder()
				.setAuthor(timedOutBy.getUser().getAsTag(), null, timedOutBy.getEffectiveAvatarUrl())
				.setTitle("Timeout")
				.setColor(slashCommandConfig.getErrorColor())
				.addField("Member", member.getAsMention(), true)
				.addField("Timed Out by", timedOutBy.getAsMention(), true)
				.addField("Until", String.format("<t:%d>", Instant.now().plus(duration).getEpochSecond()), true)
				.addField("Timeout Reason", String.format(reasonFormat, reason), false)
				.setTimestamp(Instant.now())
				.setFooter(member.getUser().getAsTag(), member.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildTimeoutRemovedEmbed(Member member, Member timedOutBy, String reason) {
		return new EmbedBuilder()
				.setAuthor(timedOutBy.getUser().getAsTag(), null, timedOutBy.getEffectiveAvatarUrl())
				.setTitle("Timeout Removed")
				.setColor(slashCommandConfig.getSuccessColor())
				.addField("Member", member.getAsMention(), true)
				.addField("Removed by", timedOutBy.getAsMention(), true)
				.addField("Timeout Reason", String.format(reasonFormat, reason), false)
				.setTimestamp(Instant.now())
				.setFooter(member.getUser().getAsTag(), member.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildClearWarnsEmbed(User user, User clearedBy) {
		return new EmbedBuilder()
				.setAuthor(clearedBy.getAsTag(), null, clearedBy.getEffectiveAvatarUrl())
				.setTitle("Warns Cleared")
				.setColor(slashCommandConfig.getWarningColor())
				.setDescription("All warns have been cleared from " + user.getAsMention() + "'s record.")
				.setTimestamp(Instant.now())
				.setFooter(user.getAsTag(), user.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildClearWarnsByIdEmbed(Warn w, User clearedBy) {
		return new EmbedBuilder()
				.setAuthor(clearedBy.getAsTag(), null, clearedBy.getEffectiveAvatarUrl())
				.setTitle("Warn Cleared")
				.setColor(slashCommandConfig.getWarningColor())
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

	private MessageEmbed buildBanEmbed(Member member, String reason, Member bannedBy) {
		return new EmbedBuilder()
				.setAuthor(bannedBy.getUser().getAsTag(), null, bannedBy.getEffectiveAvatarUrl())
				.setTitle("Ban")
				.setColor(slashCommandConfig.getErrorColor())
				.addField("User", member.getAsMention(), true)
				.addField("Banned by", bannedBy.getAsMention(), true)
				.addField("Ban Reason", String.format(reasonFormat, reason), false)
				.setTimestamp(Instant.now())
				.setFooter(member.getUser().getAsTag(), member.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildUnbanEmbed(long userId, Member unbannedBy) {
		return new EmbedBuilder()
				.setAuthor(unbannedBy.getUser().getAsTag(), null, unbannedBy.getEffectiveAvatarUrl())
				.setTitle("Ban Revoked")
				.setColor(slashCommandConfig.getErrorColor())
				.addField("Unbanned by", unbannedBy.getAsMention(), true)
				.addField("User Id", String.format(reasonFormat, userId), false)
				.setTimestamp(Instant.now())
				.build();
	}
}