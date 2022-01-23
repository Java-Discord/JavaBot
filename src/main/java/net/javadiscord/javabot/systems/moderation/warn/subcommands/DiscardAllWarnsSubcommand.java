package net.javadiscord.javabot.systems.moderation.warn.subcommands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.moderation.ModerationService;

/**
 * Subcommand that allows staff-members to discard all warns from a user.
 */
public class DiscardAllWarnsSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var userOption = event.getOption("user");
		if (userOption == null) return Responses.error(event, "User cannot be empty!");
		var user = userOption.getAsUser();
		var moderationService = new ModerationService(event.getInteraction());
		moderationService.discardAllWarns(user, event.getUser());
		return Responses.success(event, "Warns Discarded", String.format("Discarded all warns from %s.", user.getAsTag()));
	}
}

