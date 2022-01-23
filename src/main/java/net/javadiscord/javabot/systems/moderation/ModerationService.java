package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.Interaction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.util.TimeUtils;

import java.awt.*;
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
	private final ModerationConfig config;

	/**
	 * Constructs the service.
	 *
	 * @param jda    The API to use to interact with various discord entities.
	 * @param config The moderation config to use.
	 */
	public ModerationService(JDA jda, ModerationConfig config) {
		this.jda = jda;
		this.config = config;
	}

	/**
	 * Constructs the service using information obtained from an interaction.
	 *
	 * @param interaction The interaction to use.
	 */
	public ModerationService(Interaction interaction) {
		this(
				interaction.getJDA(),
				Bot.config.get(interaction.getGuild()).getModeration()
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
	public void warn(Member member, WarnSeverity severity, String reason, Member warnedBy, TextChannel channel, boolean quiet) {
		DbHelper.doDbAction(con -> {
			var repo = new WarnRepository(con);
			var warn = repo.insert(new Warn(member.getIdLong(), warnedBy.getIdLong(), severity, reason));
			LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getWarnTimeoutDays());
			int totalWeight = repo.getTotalSeverityWeight(member.getIdLong(), cutoff);
			var warnEmbed = buildWarnEmbed(member, severity, warn.getId(), reason, warnedBy, warn.getCreatedAt().toInstant(ZoneOffset.UTC), totalWeight);
			member.getUser().openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(warnEmbed).queue(),
					e -> log.info("Could not send Direct Message to User {}", member.getUser().getAsTag())
			);
			config.getLogChannel().sendMessageEmbeds(warnEmbed).queue();
			if (!quiet && channel.getIdLong() != config.getLogChannelId()) {
				channel.sendMessageEmbeds(warnEmbed).queue();
			}
			if (totalWeight > config.getMaxWarnSeverity()) {
				ban(member, "Too many warns.", warnedBy, channel, quiet);
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
			var embed = buildClearWarnsEmbed(user, clearedBy);
			user.openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(embed).queue());
			config.getLogChannel().sendMessageEmbeds(embed).queue();
		});
	}

	/**
	 * Clears a warn by discarding the Warn with the corresponding id.
	 *
	 * @param id        The id of the warn to discard.
	 * @param clearedBy The user who cleared the warn.
	 */
	public boolean discardWarnById(long id, User clearedBy) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new WarnRepository(con);
			if (repo.findById(id).isEmpty()) return false;
			var w = repo.findById(id).get();
			repo.discardById(w.getId());
			var embed = buildClearWarnsByIdEmbed(w, clearedBy);
			config.getLogChannel().sendMessageEmbeds(embed).queue();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List<Warn> getWarns(long userId) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new WarnRepository(con);
			LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getWarnTimeoutDays());
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
	public boolean timeout(Member member, String reason, Member timedOutBy, Duration duration, TextChannel channel, boolean quiet) {
		var timeoutEmbed = buildTimeoutEmbed(member, timedOutBy, reason, duration);
		if (canTimeoutUser(member, timedOutBy)) {
			member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(timeoutEmbed).queue(),
					e -> log.info("Could not send Direct Message to User {}", member.getUser().getAsTag())
			);
			channel.getGuild().timeoutFor(member, duration).queue();
			config.getLogChannel().sendMessageEmbeds(timeoutEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(timeoutEmbed).queue();
			return true;
		}
		return false;
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
	public boolean removeTimeout(Member member, String reason, Member removedBy, TextChannel channel, boolean quiet) {
		var removeTimeoutEmbed = buildTimeoutRemovedEmbed(member, removedBy, reason);
		if (canTimeoutUser(member, removedBy)) {
			member.getUser().openPrivateChannel().queue(c -> c.sendMessageEmbeds(removeTimeoutEmbed).queue(),
					e -> log.info("Could not send Direct Message to User {}", member.getUser().getAsTag())
			);
			channel.getGuild().removeTimeout(member).queue();
			config.getLogChannel().sendMessageEmbeds(removeTimeoutEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(removeTimeoutEmbed).queue();
			return true;
		}
		return false;
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
	public boolean ban(Member member, String reason, Member bannedBy, TextChannel channel, boolean quiet) {
		var banEmbed = buildBanEmbed(member, reason, bannedBy);
		if (canBanUser(member, bannedBy)) {
			member.getUser().openPrivateChannel().queue(
					c -> c.sendMessage(config.getBanMessageText()).setEmbeds(banEmbed).queue(),
					e -> log.info("Could not send Direct Message to User {}", member.getUser().getAsTag())
			);
			channel.getGuild().ban(member, BAN_DELETE_DAYS, reason).queue();
			config.getLogChannel().sendMessageEmbeds(banEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(banEmbed).queue();
			return true;
		}
		return false;
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
	public boolean unban(long userId, Member bannedBy, TextChannel channel, boolean quiet) {
		var unbanEmbed = buildUnbanEmbed(userId, bannedBy);
		if (isBanned(channel.getGuild(), userId)) {
			channel.getGuild().unban(User.fromId(userId)).queue();
			config.getLogChannel().sendMessageEmbeds(unbanEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(unbanEmbed).queue();
			return true;
		} else return false;
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
	public boolean kick(Member member, String reason, Member kickedBy, TextChannel channel, boolean quiet) {
		var kickEmbed = buildKickEmbed(member, kickedBy, reason);
		if (canKickUser(member, kickedBy)) {
			member.getUser().openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(kickEmbed).queue(),
					e -> log.info("Could not send Direct Message to User {}", member.getUser().getAsTag())
			);
			channel.getGuild().kick(member).queue();
			config.getLogChannel().sendMessageEmbeds(kickEmbed).queue();
			if (!quiet) channel.sendMessageEmbeds(kickEmbed).queue();
			return true;
		} else return false;
	}

	private MessageEmbed buildWarnEmbed(Member member, WarnSeverity severity, long warnId, String reason, Member warnedBy, Instant timestamp, int totalSeverity) {
		return new EmbedBuilder()
				.setColor(Color.ORANGE)
				.setTitle(String.format("`%d` %s | Warn (%d/%d)", warnId, member.getUser().getAsTag(), totalSeverity, config.getMaxWarnSeverity()))
				.addField("User", member.getAsMention(), true)
				.addField("Warned by", warnedBy.getAsMention(), true)
				.addField("Severity", String.format("`%s (%s)`", severity.name(), severity.getWeight()), true)
				.addField("Reason", String.format("```\n%s\n```", reason), false)
				.setTimestamp(timestamp)
				.setFooter(warnedBy.getUser().getAsTag(), warnedBy.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildKickEmbed(Member member, Member kickedBy, String reason) {
		return new EmbedBuilder()
				.setColor(Color.RED)
				.setTitle(String.format("%s | Kick", member.getUser().getAsTag()))
				.addField("User", member.getAsMention(), true)
				.addField("Kicked by", kickedBy.getAsMention(), true)
				.addField("Reason", String.format("```\n%s\n```", reason), false)
				.setTimestamp(Instant.now())
				.setFooter(kickedBy.getUser().getAsTag(), kickedBy.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildTimeoutEmbed(Member member, Member timedOutBy, String reason, Duration duration) {
		return new EmbedBuilder()
				.setColor(Color.RED)
				.setTitle(String.format("%s | Timeout", member.getUser().getAsTag()))
				.addField("User", member.getAsMention(), true)
				.addField("Timed Out by", timedOutBy.getAsMention(), true)
				.addField("Until", String.format("`%s UTC`", LocalDateTime.now().plus(duration).format(TimeUtils.STANDARD_FORMATTER)), true)
				.addField("Reason", String.format("```\n%s\n```", reason), false)
				.setTimestamp(Instant.now())
				.setFooter(timedOutBy.getUser().getAsTag(), timedOutBy.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildTimeoutRemovedEmbed(Member member, Member timedOutBy, String reason) {
		return new EmbedBuilder()
				.setColor(Color.GREEN)
				.setTitle(String.format("%s | Timeout Removed", member.getUser().getAsTag()))
				.addField("User", member.getAsMention(), true)
				.addField("Timeout removed by", timedOutBy.getAsMention(), true)
				.addField("Reason", String.format("```\n%s\n```", reason), false)
				.setTimestamp(Instant.now())
				.setFooter(timedOutBy.getUser().getAsTag(), timedOutBy.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildClearWarnsEmbed(User user, User clearedBy) {
		return new EmbedBuilder()
				.setColor(Color.ORANGE)
				.setTitle(String.format("%s | Warns Cleared", user.getAsTag()))
				.setDescription("All warns have been cleared from " + user.getAsMention() + "'s record.")
				.setTimestamp(Instant.now())
				.setFooter(clearedBy.getAsTag(), clearedBy.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildClearWarnsByIdEmbed(Warn w, User clearedBy) {
		return new EmbedBuilder()
				.setColor(Color.ORANGE)
				.setTitle("Warn Cleared")
				.setDescription(String.format("""
								Cleared the following warn from <@%s>'s record:

								`%s` <t:%s>
								Warned by: <@%s>
								Severity: `%s (%s)`
								Reason: %s""",
						w.getUserId(), w.getId(), w.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond(),
						w.getWarnedBy(), w.getSeverity(), w.getSeverityWeight(), w.getReason()))
				.setTimestamp(Instant.now())
				.setFooter(clearedBy.getAsTag(), clearedBy.getEffectiveAvatarUrl())
				.build();
	}

	private MessageEmbed buildBanEmbed(Member member, String reason, Member bannedBy) {
		return new EmbedBuilder()
				.setColor(Color.RED)
				.setTitle(String.format("%s | Ban", member.getUser().getAsTag()))
				.addField("User", member.getAsMention(), true)
				.addField("Banned by", bannedBy.getAsMention(), true)
				.addField("Reason", String.format("```\n%s\n```", reason), false)
				.setTimestamp(Instant.now())
				.setFooter(bannedBy.getUser().getAsTag(), bannedBy.getEffectiveAvatarUrl())
				.build();
	}

	public MessageEmbed buildUnbanEmbed(long userId, Member unbannedBy) {
		return new EmbedBuilder()
				.setAuthor("Unban")
				.setColor(Color.RED)
				.addField("Unbanned by", unbannedBy.getAsMention(), true)
				.addField("User Id", String.format("```\n%s\n```", userId), false)
				.setTimestamp(Instant.now())
				.setFooter(unbannedBy.getUser().getAsTag(), unbannedBy.getEffectiveAvatarUrl())
				.build();
	}
}