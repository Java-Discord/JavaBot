package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.moderation.ModerateUserCommand;

/**
 * Command that allows staff-members to kick members.
 */
public class KickCommand extends ModerateUserCommand {
	@Override
	protected ReplyCallbackAction handleModerationActionCommand(SlashCommandInteractionEvent event, Member commandUser, Member target) throws ResponseException {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		if (userOption == null || reasonOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var member = userOption.getAsMember();
		if (member == null) {
			return Responses.error(event, "Cannot kick a user who is not a member of this server");
		}
		var reason = reasonOption.getAsString();
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.kick(member, reason, event.getMember(), event.getTextChannel(), quiet)) {
			return Responses.success(event, "User Kicked", String.format("%s has been kicked.", member.getAsMention()));
		} else {
			return Responses.warning(event, "You're not permitted to kick this user.");
		}
	}
}