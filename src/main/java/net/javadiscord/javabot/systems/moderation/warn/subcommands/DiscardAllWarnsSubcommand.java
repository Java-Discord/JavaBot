package net.javadiscord.javabot.systems.moderation.warn.subcommands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.moderation.ModerateMemberCommand;
import net.javadiscord.javabot.systems.moderation.ModerationService;

/**
 * Subcommand that allows staff-members to discard all warns from a user.
 */
public class DiscardAllWarnsSubcommand extends ModerateMemberCommand {

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(SlashCommandInteractionEvent event, Member commandUser, Member target) throws ResponseException {
		var moderationService = new ModerationService(event.getInteraction());
		moderationService.discardAllWarns(target.getUser(), commandUser.getUser());
		return Responses.success(event, "Warns Discarded", String.format("Discarded all warns from %s.", target.getUser().getAsTag()));
	}
}

