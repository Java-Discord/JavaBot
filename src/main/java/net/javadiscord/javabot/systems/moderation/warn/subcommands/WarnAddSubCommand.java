package net.javadiscord.javabot.systems.moderation.warn.subcommands;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;

public class WarnAddSubCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var userOption = event.getOption("user");
		var reasonOption = event.getOption("reason");
		var severityOption = event.getOption("severity");
		if (userOption == null || reasonOption == null || severityOption == null) {
			return Responses.error(event, "Missing required Arguments.");
		}
		var member = userOption.getAsMember();
		var severity = WarnSeverity.valueOf(severityOption.getAsString().trim().toUpperCase());
		if (member.getUser().isBot()) return Responses.warning(event, "Cannot warn Bots.");

		if (event.getChannel().getType() != ChannelType.TEXT) {
			return Responses.error(event, "This command can only be performed in a server text channel.");
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		var moderationService = new ModerationService(event.getInteraction());
		moderationService.warn(member, severity, reasonOption.getAsString(), event.getMember(), event.getTextChannel(), quiet);
		return Responses.success(event, "User Warned", String.format("User %s has been warned.", member.getUser().getAsTag()));
	}
}

