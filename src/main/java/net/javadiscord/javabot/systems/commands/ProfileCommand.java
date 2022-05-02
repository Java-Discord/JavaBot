package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.model.Warn;
import net.javadiscord.javabot.systems.qotw.dao.QuestionPointsRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Command that allows members to display info about themselves or other users.
 */
public class ProfileCommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		Member member = event.getOption("user", event::getMember, OptionMapping::getAsMember);
		try {
			return event.replyEmbeds(buildProfileEmbed(member, Bot.dataSource.getConnection()));
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "Could not load profile.");
		}
	}

	private MessageEmbed buildProfileEmbed(Member member, Connection con) throws SQLException {
		var config = Bot.config.get(member.getGuild());
		var warns = new ModerationService(member.getJDA(), config).getWarns(member.getIdLong());
		var points = new QuestionPointsRepository(con).getAccountByUserId(member.getIdLong()).getPoints();
		var roles = member.getRoles();
		var status = member.getOnlineStatus().name();
		var helpXP = new HelpExperienceService(Bot.dataSource).getOrCreateAccount(member.getIdLong()).getExperience();
		var embed = new EmbedBuilder()
				.setTitle("Profile")
				.setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
				.setDescription(getDescription(member))
				.setColor(member.getColor())
				.setThumbnail(member.getUser().getEffectiveAvatarUrl() + "?size=4096")
				.setTimestamp(Instant.now())
				.addField("User", member.getAsMention(), true)
				.addField("Status",
						status.substring(0, 1).toUpperCase() +
								status.substring(1).toLowerCase().replace("_", " "), true)
				.addField("ID", member.getId(), true);
		if (!roles.isEmpty()) {
			embed.addField(String.format("Roles (+%s other)", roles.size() - 1), roles.get(0).getAsMention(), true);
		}
		embed.addField("Warns", String.format("`%s (%s/%s)`",
						warns.size(),
						warns.stream().mapToLong(Warn::getSeverityWeight).sum(),
						config.getModeration().getMaxWarnSeverity()), true)
				.addField("QOTW-Points", String.format("`%s point%s (#%s)`",
						points,
						points == 1 ? "" : "s",
						LeaderboardCommand.getQOTWRank(member, member.getGuild())), true)
				.addField("Total Help XP", String.format("`%.2f XP`", helpXP), true)
				.addField("Server joined", String.format("<t:%s:R>", member.getTimeJoined().toEpochSecond()), true)
				.addField("Account created", String.format("<t:%s:R>", member.getUser().getTimeCreated().toEpochSecond()), true);

		if (member.getTimeBoosted() != null) {
			embed.addField("Boosted since", String.format("<t:%s:R>", member.getTimeBoosted().toEpochSecond()), true);
		}
		return embed.build();
	}

	private String getDescription(Member member) {
		StringBuilder sb = new StringBuilder();
		if (getCustomActivity(member) != null) {
			sb.append("\n`").append(getCustomActivity(member).getName()).append("`");
		}
		if (getGameActivity(member) != null) {
			sb.append(String.format("\n%s %s",
					getGameActivityType(getGameActivity(member)),
					getGameActivityDetails(getGameActivity(member))));
		}
		return sb.toString();
	}

	private Activity getCustomActivity(Member member) {
		Activity activity = null;
		for (var act : member.getActivities()) {
			if (act.getType().name().equals("CUSTOM_STATUS")) {
				activity = act;
				break;
			}
		}
		return activity;
	}

	private Activity getGameActivity(Member member) {
		Activity activity = null;
		for (var act : member.getActivities()) {
			if (act.getType().name().equals("CUSTOM_STATUS")) {
				continue;
			} else {
				activity = act;
			}
			break;
		}
		return activity;
	}

	private String getGameActivityType(Activity activity) {
		return activity.getType().name().toLowerCase()
				.replace("listening", "Listening to")
				.replace("default", "Playing");
	}

	private String getGameActivityDetails(Activity activity) {
		StringBuilder sb = new StringBuilder();
		if (activity.getName().equals("Spotify")) {
			var rp = activity.asRichPresence();
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
