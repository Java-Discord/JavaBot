package net.javadiscord.javabot.systems.moderation.warn.subcommands;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;

/**
 * Subcommand that allows staff-members to add a single warn to any user.
 */
public class WarnAddSubcommand implements SlashCommandHandler {
	@Override
	public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		var severityOption = event.getOption("severity");
		if (userOption == null || reasonOption == null || severityOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var member = userOption.getAsMember();
		if (member == null) {
			return Responses.error(event, "Cannot warn a user who is not a member of this server");
		}
		var severity = WarnSeverity.valueOf(severityOption.getAsString().trim().toUpperCase());
		if (member.getUser().isBot()) return Responses.warning(event, "Cannot warn Bots.");

		if (event.getChannel().getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		moderationService.warn(member, severity, reasonOption.getAsString(), event.getMember(), event.getTextChannel(), quiet);
		return Responses.success(event, "User Warned", String.format("%s has been warned.", member.getAsMention()));
	}
}

