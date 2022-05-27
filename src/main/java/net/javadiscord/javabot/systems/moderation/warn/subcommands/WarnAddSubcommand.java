package net.javadiscord.javabot.systems.moderation.warn.subcommands;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.moderation.ModerateUserCommand;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;

/**
 * Subcommand that allows staff-members to add a single warn to any user.
 */
public class WarnAddSubcommand extends ModerateUserCommand {

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(SlashCommandInteractionEvent event, Member commandUser, Member target) throws ResponseException {
		var reasonOption = event.getOption("reason");
		var severityOption = event.getOption("severity");
		if (reasonOption == null || severityOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}

		var severity = WarnSeverity.valueOf(severityOption.getAsString().trim().toUpperCase());
		if (target.getUser().isBot()) return Responses.warning(event, "Cannot warn Bots.");

		if (event.getChannel().getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		moderationService.warn(target, severity, reasonOption.getAsString(), event.getMember(), event.getTextChannel(), quiet);
		return Responses.success(event, "User Warned", String.format("%s has been warned.", target.getAsMention()));
	}
}

