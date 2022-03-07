package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;

/**
 * Command that allows staff-members to unban users from the current guild by their id.
 */
public class UnbanCommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var id = idOption.getAsLong();
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		var moderationService = new ModerationService(event.getInteraction());
		if (moderationService.unban(id, event.getMember(), event.getTextChannel(), quiet)) {
			return Responses.success(event, "User Unbanned", String.format("User with id `%s` has been unbanned.", id));
		} else {
			return Responses.warning(event, String.format("Could not find banned User with id `%s`", id));
		}
	}
}