package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * <h3>This class represents the /profile command.</h3>
 */
public class ProfileCommand extends SlashCommand {
	private final QOTWPointsService qotwPointsService;
	private final NotificationService notificationService;
	private final DataSource dataSource;
	private final BotConfig botConfig;
	private final DbHelper dbHelper;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param qotwPointsService The {@link QOTWPointsService}
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param dataSource A factory for connections to the main database
	 * @param dbHelper An object managing databse operations
	 */
	public ProfileCommand(QOTWPointsService qotwPointsService, NotificationService notificationService, BotConfig botConfig, DataSource dataSource, DbHelper dbHelper) {
		this.qotwPointsService = qotwPointsService;
		this.notificationService = notificationService;
		this.dataSource = dataSource;
		this.botConfig = botConfig;
		this.dbHelper = dbHelper;
		setSlashCommandData(Commands.slash("profile", "Shows your server profile.")
				.addOption(OptionType.USER, "user", "If given, shows the profile of the user instead.", false)
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		Member member = event.getOption("user", event::getMember, OptionMapping::getAsMember);
		if (member == null) {
			Responses.error(event, "The user must be a part of this server!").queue();
			return;
		}
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		try {
			event.replyEmbeds(buildProfileEmbed(member)).queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
		}
	}

	private @NotNull MessageEmbed buildProfileEmbed(@NotNull Member member) throws SQLException {
		GuildConfig config = botConfig.get(member.getGuild());
		List<Warn> warns = new ModerationService(notificationService, config, dbHelper).getWarns(member.getIdLong());
		long points = qotwPointsService.getPoints(member.getIdLong());
		List<Role> roles = member.getRoles();
		String status = member.getOnlineStatus().name();
		double helpXP = new HelpExperienceService(dataSource, botConfig).getOrCreateAccount(member.getIdLong()).getExperience();
		EmbedBuilder embed = new EmbedBuilder()
				.setTitle("Profile")
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setDescription(getDescription(member))
				.setColor(member.getColor())
				.setThumbnail(member.getEffectiveAvatarUrl() + "?size=4096")
				.setTimestamp(Instant.now())
				.addField("User", member.getAsMention(), true)
				.addField("Status", StringUtils.capitalize(status.toLowerCase()).replace("_", " "), true)
				.addField("ID", member.getId(), true);
		if (!roles.isEmpty()) {
			embed.addField(String.format("Roles (+%s other)", roles.size() - 1), roles.get(0).getAsMention(), true);
		}
		if (!warns.isEmpty()) {
			embed.addField("Warns", String.format("`%s (%s/%s)`",
					warns.size(),
					warns.stream().mapToLong(Warn::getSeverityWeight).sum(),
					config.getModerationConfig().getMaxWarnSeverity()), true);
		}
		embed.addField("QOTW-Points", String.format("`%s point%s (#%s)`",
						points, points == 1 ? "" : "s",
						qotwPointsService.getQOTWRank(member.getIdLong())), true)
				.addField("Total Help XP", String.format("`%.2f XP`", helpXP), true)
				.addField("Server joined", String.format("<t:%s:R>", member.getTimeJoined().toEpochSecond()), true)
				.addField("Account created", String.format("<t:%s:R>", member.getUser().getTimeCreated().toEpochSecond()), true);
		if (member.getTimeBoosted() != null) {
			embed.addField("Boosted since", String.format("<t:%s:R>", member.getTimeBoosted().toEpochSecond()), true);
		}
		return embed.build();
	}

	private @NotNull String getDescription(Member member) {
		StringBuilder sb = new StringBuilder();
		getActivity(member, true).ifPresent(activity -> sb.append("\n`").append(activity.getName()).append("`"));
		getActivity(member, false).ifPresent(activity -> sb.append(String.format("\n%s %s",
				getGameActivityType(activity),
				getGameActivityDetails(activity))));
		return sb.toString();
	}

	private @NotNull Optional<Activity> getActivity(@NotNull Member member, boolean customActivity) {
		return member.getActivities().stream()
				.filter(a -> customActivity == (a.getType() == Activity.ActivityType.CUSTOM_STATUS))
				.findFirst();
	}

	private @NotNull String getGameActivityType(@NotNull Activity activity) {
		return activity.getType().name().toLowerCase()
				.replace("listening", "Listening to")
				.replace("default", "Playing");
	}

	private @NotNull String getGameActivityDetails(@NotNull Activity activity) {
		StringBuilder sb = new StringBuilder();
		if (activity.getName().equals("Spotify")) {
			RichPresence rp = activity.asRichPresence();
			String spotifyURL = "https://open.spotify.com/track/" + rp.getSyncId();
			sb.append(String.format("[`\"%s\"", rp.getDetails()));
			if (rp.getState() != null) sb.append(" by " + rp.getState());
			sb.append(String.format("`](%s)", spotifyURL));
		} else {
			sb.append(String.format("`%s`", activity.getName()));
		}
		return sb.toString();
	}

}
