package net.javadiscord.javabot.command.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;

/**
 * A moderation command action like ban, kick, mute, report, etc. | In short, it targets a user.
 */
public abstract class ModerateUserCommand extends ModerateCommand {
	private boolean actOnSelf;

	public ModerateUserCommand() {
		setActOnSelf(false);
	}

	public ModerateUserCommand(boolean actOnSelf) {
		setActOnSelf(actOnSelf);
	}

	protected void setActOnSelf(boolean actOnSelf) {
		this.actOnSelf = actOnSelf;
	}

	@Override
	protected final ReplyCallbackAction handleModerationCommand(SlashCommandInteractionEvent event, Member commandUser) throws ResponseException {
		OptionMapping targetOption = event.getOption("user");
		if (targetOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		Member target = targetOption.getAsMember();
		if (target == null) {
			return Responses.error(event, "Cannot perform action on an user that isn't in the server.");
		}

		if (!actOnSelf || !commandUser.isOwner()) {
			if (commandUser.getId().equals(target.getId())) {
				return Responses.error(event, "You cannot perform this action on yourself.");
			}
		}

		if (target.isOwner()) {
			return Responses.error(event, "You cannot preform actions on a higher member staff member.");
		} else {
			//If both users have at least one role.
			if (target.getRoles().size() > 0 && commandUser.getRoles().size() > 0) {
				if (commandUser.getRoles().get(0).getPosition() <= target.getRoles().get(0).getPosition()) {
					return Responses.error(event, "You cannot preform actions on a higher/equal member staff member.");
				}
			}
		}

		//CommandUser role less than or equal to target role

		return handleModerationActionCommand(event, commandUser, target);
	}

	protected abstract ReplyCallbackAction handleModerationActionCommand(SlashCommandInteractionEvent event, Member commandUser, Member target) throws ResponseException;
}
