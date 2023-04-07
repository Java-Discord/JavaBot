package net.javadiscord.javabot.systems.qotw.commands.qotw_points;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;

/**
 * <h3>This class represents the /qotw account increment command.</h3>
 * This Subcommand allows staff-members to increment the QOTW-points of any user.
 */
public class IncrementPointsSubcommand extends ChangePointsSubcommand {

	public IncrementPointsSubcommand(QOTWPointsService pointsService, NotificationService notificationService) {
		super(pointsService, notificationService, "increment", "Adds one point to the user's QOTW-Account");
	}

	@Override
	protected int getIncrementCount(Member targetMember, SlashCommandInteractionEvent event) {
		return 1;
	}

	@Override
	protected @NotNull EmbedBuilder createIncrementEmbedBuilder(User user, long points) {
		return super.createIncrementEmbedBuilder(user, points)
				.setTitle("QOTW Account incremented");
	}

}
