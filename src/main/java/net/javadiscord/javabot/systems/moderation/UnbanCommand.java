package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

public class UnbanCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.error(event, "Missing required Arguments.");
		}
		var id = idOption.getAsLong();
		var channel = event.getTextChannel();
		if (channel.getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.unban(id, event.getMember(), channel, quiet)) {
			return Responses.success(event, "User Unbanned", String.format("User with id `%s` has been unbanned.", id));
		} else {
			return Responses.warning(event, String.format("Could not find banned User with id `%s`", id));
		}
	}
}


