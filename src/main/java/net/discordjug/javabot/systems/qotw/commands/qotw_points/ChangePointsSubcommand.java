package net.discordjug.javabot.systems.qotw.commands.qotw_points;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.systems.notification.QOTWNotificationService;
import net.discordjug.javabot.systems.qotw.QOTWPointsService;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * This is an abstract class for QOTW subcommands allowing staff-members to change the QOTW-points of any user.
 */
public abstract class ChangePointsSubcommand extends SlashCommand.Subcommand {

	private final QOTWPointsService pointsService;
	private final NotificationService notificationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param pointsService The {@link QOTWPointsService}
	 * @param notificationService The {@link NotificationService}
	 * @param commandName The name of the command
	 * @param commandDescription The description of the command
	 */
	public ChangePointsSubcommand(QOTWPointsService pointsService, NotificationService notificationService, String commandName, String commandDescription) {
		this.pointsService = pointsService;
		this.notificationService = notificationService;
		setCommandData(new SubcommandData(commandName, commandDescription)
				.addOption(OptionType.USER, "user", "The user whose points should be changed.", true)
				.addOption(OptionType.BOOLEAN, "quiet", "Whether or not the user should be notified", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping userOption = event.getOption("user");
		if (userOption == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		Member member = userOption.getAsMember();
		if (member == null) {
			Responses.error(event, "User must be a part of this server.").queue();
			return;
		}
		boolean quiet = event.getOption("quiet", () -> false, OptionMapping::getAsBoolean);
		event.deferReply().queue();
		long points = changePoints(member, event);
		sendNotifications(event, member, points, quiet);
	}

	private long changePoints(Member targetMember, SlashCommandInteractionEvent event) {
		return pointsService.increment(targetMember.getIdLong(), getIncrementCount(targetMember, event));
	}

	protected abstract int getIncrementCount(Member targetMember, SlashCommandInteractionEvent event);

	private void sendNotifications(SlashCommandInteractionEvent event, Member member, long points, boolean quiet) {
		MessageEmbed embed = buildIncrementEmbed(member.getUser(), points);
		notificationService.withGuild(event.getGuild()).sendToModerationLog(c -> c.sendMessageEmbeds(embed));
		if (!quiet) {
			sendUserNotification(notificationService.withQOTW(event.getGuild(), member.getUser()), member);

		}
		event.getHook().sendMessageEmbeds(embed).queue();
	}

	protected abstract void sendUserNotification(@NotNull QOTWNotificationService notificationService, Member member);

	protected @NotNull MessageEmbed buildIncrementEmbed(@NotNull User user, long points) {
		return createIncrementEmbedBuilder(user, points)
				.build();
	}

	/**
	 * Creates an {@link EmbedBuilder} for the notification embed.
	 * @param user The user whose account is incremented
	 * @param points The new total number of points of the user
	 * @return The created {@link EmbedBuilder} for creating the notification embed
	 */
	protected @NotNull EmbedBuilder createIncrementEmbedBuilder(User user, long points) {
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(user), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Account changed")
				.setColor(Responses.Type.SUCCESS.getColor())
				.addField("Total QOTW-Points", "```" + points + "```", true)
				.addField("Rank", "```#" + pointsService.getQOTWRank(user.getIdLong()) + "```", true)
				.setFooter("ID: " + user.getId())
				.setTimestamp(Instant.now());
	}
}
