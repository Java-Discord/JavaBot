package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.moderation.ModerateCommand;
import net.javadiscord.javabot.command.moderation.UserPermissionException;

/**
 * Command that allows staff-members to ban guild members.
 */
public class BanCommand extends ModerateCommand {

	@Override
	protected ReplyCallbackAction handleModerationCommand(SlashCommandInteractionEvent event, Member commandUser) throws ResponseException {
		var reasonOption = event.getOption("reason");
		var targetOption = event.getOption("user");

		if (reasonOption == null || targetOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}

		String reason = reasonOption.getAsString();
		Member target = targetOption.getAsMember();
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);

		if (target == null) {
			String snowflakeID = targetOption.getAsString();
			return handleAsID(event, commandUser, snowflakeID, reason, quiet);
		}

		if (target.getId().equals(commandUser.getId())) {
			Responses.error(event, "You cannot ban yourself.");
		}

		if (event.getJDA().getSelfUser().getId().equals(target.getId())) {
			Responses.error(event, "You cannot ban the bot.");
		}

		return handleAsMember(event, commandUser, target, reason, quiet);
	}

	private ReplyCallbackAction handleAsID(SlashCommandInteractionEvent event, Member commandUser, String snowflakeID, String reason, boolean quiet) {
		ModerationService moderationService = new ModerationService(event.getInteraction());

		try {
			moderationService.ban(snowflakeID, reason, commandUser, event.getTextChannel(), quiet);
		} catch (IllegalArgumentException e) {
			return Responses.error(event, "Incorrect Argument");
		} catch (UserPermissionException e) {
			return Responses.error(event, e.toString());
		}

		return Responses.success(event, "User Banned", String.format("%s has been banned.", snowflakeID));
	}

	private ReplyCallbackAction handleAsMember(SlashCommandInteractionEvent event, Member commandUser, Member target, String reason, boolean quiet) {
		ModerationService moderationService = new ModerationService(event.getInteraction());

		try {
			moderationService.ban(target, reason, commandUser, event.getTextChannel(), quiet);
		} catch (IllegalArgumentException e) {
			return Responses.error(event, "Incorrect Argument");
		} catch (UserPermissionException e) {
			return Responses.error(event, e.toString());
		}

		return Responses.success(event, "User Banned", String.format("%s has been banned.", target.getAsMention()));
	}
}