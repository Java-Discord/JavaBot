package net.javadiscord.javabot.systems.moderation.timeout.subcommands;


import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.moderation.ModerationService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AddTimeoutSubCommand implements SlashCommandHandler {

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		var durationAmountOption = event.getOption("duration-amount");
		var durationTimeUnitOption = event.getOption("duration-timeunit");
		if (userOption == null || reasonOption == null || durationAmountOption == null || durationTimeUnitOption == null) {
			return Responses.error(event, "Missing required Arguments.");
		}
		var member = userOption.getAsMember();
		if (member == null) {
			return Responses.error(event, "Cannot timeout a user who is not a member of this server");
		}
		var reason = reasonOption.getAsString();
		var duration = Duration.of(durationAmountOption.getAsLong(), ChronoUnit.valueOf(durationTimeUnitOption.getAsString()));
		if (duration.toSeconds() > (Member.MAX_TIME_OUT_LENGTH * 24 * 60 * 60)) {
			return Responses.error(event, String.format("You cannot add a Timeout longer than %s days.", Member.MAX_TIME_OUT_LENGTH));
		}
		var channel = event.getTextChannel();
		if (channel.getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		if (member.isTimedOut()) {
			return Responses.error(event, String.format("Could not timeout %s; they are already timed out.", member.getAsMention()));
		}
		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.timeout(member, reason, event.getMember(), duration, channel, quiet)) {
			return Responses.success(event, "User Timed Out", String.format("%s has been timed out.", member.getAsMention()));
		} else {
			return Responses.warning(event, "You're not permitted to time out this user.");
		}
	}
}