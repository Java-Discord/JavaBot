package net.javadiscord.javabot.systems.moderation.timeout.subcommands;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.moderation.ModerationService;

/**
 * Subcommand that allows staff-members to manually remove a timeout.
 */
public class RemoveTimeoutSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		if (userOption == null || reasonOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var member = userOption.getAsMember();
		if (member == null) {
			return Responses.error(event, "Cannot remove a timeout of a user who is not a member of this server");
		}
		var reason = reasonOption.getAsString();
		var channel = event.getTextChannel();
		if (channel.getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		if (!member.isTimedOut()) {
			return Responses.error(event, String.format("Could not remove Timeout from member %s; they are not timed out.", member.getAsMention()));
		}
		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.removeTimeout(member, reason, event.getMember(), channel, quiet)) {
			return Responses.success(event, "Timeout Removed", String.format("%s's Timeout has been removed.", member.getAsMention()));
		} else {
			return Responses.warning(event, "You're not permitted to remove Timeouts from this user.");
		}
	}
}
