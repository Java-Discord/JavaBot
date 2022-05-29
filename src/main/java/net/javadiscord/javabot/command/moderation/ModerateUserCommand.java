package net.javadiscord.javabot.command.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A moderation command action like ban, kick, mute, report, etc. | In short, it targets a user.
 */
public abstract class ModerateUserCommand extends ModerateCommand {
	private boolean actOnSelf = false;
	private boolean requireReason = true;

	public ModerateUserCommand() {
		setActOnSelf(false);
	}

	protected void setActOnSelf(boolean actOnSelf) {
		this.actOnSelf = actOnSelf;
	}

	protected void setRequireReason(boolean requireReason) {
		this.requireReason = requireReason;
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(SlashCommandInteractionEvent event, Member commandUser) {
		OptionMapping targetOption = event.getOption("user");
		OptionMapping reasonOption = event.getOption("reason");
		if (targetOption == null || (targetOption == null && requireReason)) {
			return Responses.error(event, "Missing required arguments.");
		}
		Member target = targetOption.getAsMember();
		String reason = reasonOption == null ? null : reasonOption.getAsString();
		if (target == null) {
			return Responses.error(event, "This command may only be used inside servers.");
		}
		if ((!actOnSelf || !commandUser.isOwner()) && commandUser.getIdLong() == target.getIdLong()) {
			return Responses.error(event, "You cannot perform this action on yourself.");
		}
		if (target.isOwner() || !commandUser.canInteract(target)) {
			return Responses.error(event, "You cannot perform actions on a higher member staff member.");
		}
		return handleModerationActionCommand(event, commandUser, target, reason);
	}

	protected abstract ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull Member target, @Nullable String reason);
}
