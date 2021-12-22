package net.javadiscord.javabot.systems.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.Interaction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;

import java.awt.Color;
import java.sql.SQLException;
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
	 * @param jda The API to use to interact with various discord entities.
	 * @param config The moderation config to use.
	 */
	public ModerationService(JDA jda, ModerationConfig config) {
		this.jda = jda;
		this.config = config;
	}

	/**
	 * Constructs the service using information obtained from an interaction.
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
	 * @param user The user to warn.
	 * @param severity The severity of the warning.
	 * @param reason The reason for this warning.
	 * @param warnedBy The user who issued the warning.
	 * @param channel The channel in which the warning was issued.
	 * @param quiet If true, don't send a message in the channel.
	 */
	public void warn(User user, WarnSeverity severity, String reason, User warnedBy, TextChannel channel, boolean quiet) {
		DbHelper.doDbAction(con -> {
			var repo = new WarnRepository(con);
			var warn = repo.insert(new Warn(user.getIdLong(), warnedBy.getIdLong(), severity, reason));
			LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getWarnTimeoutDays());
			int totalWeight = repo.getTotalSeverityWeight(user.getIdLong(), cutoff);
			var warnEmbed = buildWarnEmbed(user, severity, reason, warnedBy, warn.getCreatedAt().toInstant(ZoneOffset.UTC), totalWeight);
			user.openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(warnEmbed).queue());
			config.getLogChannel().sendMessageEmbeds(warnEmbed).queue();
			if (!quiet && channel.getIdLong() != config.getLogChannelId()) {
				channel.sendMessageEmbeds(warnEmbed).queue();
			}
			if (totalWeight > config.getMaxWarnSeverity()) {
				ban(user, "Too many warnings.", warnedBy, channel, quiet);
			}
		});
	}

	/**
	 * Clears warns from the given user by discarding all warns.
	 * @param user The user to clear warns from.
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
	 * Clears a warn by discarding the warn with the corresponding id.
	 * @param id The id of the warn to discard.
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
	 * Bans a user.
	 * @param user The user to ban.
	 * @param reason The reason for banning the user.
	 * @param bannedBy The user who is responsible for banning this user.
	 * @param channel The channel in which the ban was issued.
	 * @param quiet If true, don't send a message in the channel.
	 */
	public void ban(User user, String reason, User bannedBy, TextChannel channel, boolean quiet) {
		var banEmbed = buildBanEmbed(user, reason, bannedBy);
		if (canBanUser(user, bannedBy)) {
			channel.getGuild().ban(user, BAN_DELETE_DAYS, reason).queue();
			user.openPrivateChannel().queue(pc -> pc.sendMessageEmbeds(banEmbed).queue());
			if (!quiet) channel.sendMessageEmbeds(banEmbed).queue();
		} else throw new PermissionException("You don't have permission to ban this user.");
	}

	private boolean canBanUser(User user, User bannedBy) {
		var member = config.getGuild().getMember(bannedBy);
		if (member == null) throw new IllegalArgumentException("Could not retrieve Guild Member from User object: " + user.toString());
		var perms = member.getPermissions();
		if (perms.isEmpty()) return false;
		return perms.contains(Permission.BAN_MEMBERS);
	}

	public void mute(Member member, Guild guild) {
		Role muteRole = Bot.config.get(guild).getModeration().getMuteRole();
		guild.addRoleToMember(member.getId(), muteRole).queue();
	}

	private MessageEmbed buildWarnEmbed(User user, WarnSeverity severity, String reason, User warnedBy, Instant timestamp, int totalSeverity) {
		return new EmbedBuilder()
				.setColor(Color.ORANGE)
				.setTitle(String.format("%s | Warn (%d/%d)", user.getAsTag(), totalSeverity, config.getMaxWarnSeverity()))
				.addField("Warned by", warnedBy.getAsMention(), true)
				.addField("Severity", String.format("`%s (%s)`", severity.name(), severity.getWeight()), true)
				.addField("Reason", String.format("```%s```", reason), false)
				.setTimestamp(timestamp)
				.setFooter(warnedBy.getAsTag(), warnedBy.getEffectiveAvatarUrl())
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

	private MessageEmbed buildBanEmbed(User user, String reason, User bannedBy) {
		return new EmbedBuilder()
				.setColor(Color.RED)
				.setTitle(String.format("%s | Ban", user.getAsTag()))
				.addField("Reason", reason, false)
				.setTimestamp(Instant.now())
				.setFooter(bannedBy.getAsTag(), bannedBy.getEffectiveAvatarUrl())
				.build();
	}
}