package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

/**
 * Command that allows staff-members to ban guild members.
 */
public class BanCommand implements SlashCommandHandler {
	@Override
	public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		if (userOption == null || reasonOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var member = userOption.getAsMember();
		if (member == null) {
			return Responses.error(event, "Cannot ban a user who is not a member of this server");
		}
		var reason = reasonOption.getAsString();
		var channel = event.getTextChannel();
		if (channel.getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.ban(member, reason, event.getMember(), channel, quiet)) {
			return Responses.success(event, "User Banned", String.format("%s has been banned.", member.getAsMention()));
		} else {
			return Responses.warning(event, "You're not permitted to ban this user.");
		}
	}
}