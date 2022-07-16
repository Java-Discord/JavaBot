package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An abstraction of {@link ModerateCommand} which is used for commands that require a user.
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
	protected ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, Member commandUser) {
		OptionMapping targetOption = event.getOption("user");
		OptionMapping reasonOption = event.getOption("reason");
		if (targetOption == null || reasonOption == null && requireReason) {
			return Responses.error(event, "Missing required arguments.");
		}
		User target = targetOption.getAsUser();
		String reason = reasonOption == null ? null : reasonOption.getAsString();
		if ((!actOnSelf || !commandUser.isOwner()) && commandUser.getIdLong() == target.getIdLong()) {
			return Responses.error(event, "You cannot perform this action on yourself.");
		}
		// TODO: Re-implement perm-checks
		//if (target.isOwner() || !commandUser.canInteract(target)) {
		//	return Responses.error(event, "You cannot perform actions on a higher member staff member.");
		//}
		return handleModerationActionCommand(event, commandUser, target, reason);
	}

	protected abstract ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason);
}
