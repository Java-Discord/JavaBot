package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

public class BanCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		if (userOption == null || reasonOption == null) {
			return Responses.error(event, "Missing required Arguments.");
		}
		var member = userOption.getAsMember();
		var reason = reasonOption.getAsString();
		var channel = event.getTextChannel();
		if (channel.getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}

		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.ban(member, reason, event.getMember(), channel, quiet)) {
			return Responses.success(event, "User Banned", String.format("User %s has been banned.", member.getUser().getAsTag()));
		} else {
			return Responses.warning(event, "You're not permitted to ban this user.");
		}
	}
}