package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.systems.help.HelpExperienceService;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.systems.moderation.warn.model.Warn;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.StringUtils;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * <h3>This class represents the /profile command.</h3>
 */
public class ProfileCommand extends SlashCommand {
	private final QOTWPointsService qotwPointsService;
	private final BotConfig botConfig;
	private final HelpExperienceService helpExperienceService;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param qotwPointsService The {@link QOTWPointsService}
	 * @param botConfig The main configuration of the bot
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 * @param moderationService Service object for moderating members
	 */
	public ProfileCommand(QOTWPointsService qotwPointsService, BotConfig botConfig, HelpExperienceService helpExperienceService, ModerationService moderationService) {
		this.qotwPointsService = qotwPointsService;
		this.botConfig = botConfig;
		this.helpExperienceService = helpExperienceService;
		this.moderationService = moderationService;
		setCommandData(Commands.slash("profile", "Shows your server profile.")
				.addOption(OptionType.USER, "user", "If given, shows the profile of the user instead.", false)
				.setContexts(InteractionContextType.GUILD)
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
		List<Warn> warns = moderationService.getWarns(member.getGuild(), member.getIdLong());
		long points = qotwPointsService.getPoints(member.getIdLong());
		List<Role> roles = member.getRoles();
		String status = member.getOnlineStatus().name();
		double helpXP = helpExperienceService.getOrCreateAccount(member.getIdLong()).getExperience();
		EmbedBuilder embed = new EmbedBuilder()
				.setTitle("Profile")
				.setAuthor(UserUtils.getUserTag(member.getUser()), null, member.getEffectiveAvatarUrl())
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
