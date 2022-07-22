package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An abstraction of {@link ModerateCommand} which is used for commands that require a user.
 */
public abstract class ModerateUserCommand extends ModerateCommand {
	@Override
	protected ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member moderator) {
		OptionMapping targetOption = event.getOption("user");
		OptionMapping reasonOption = event.getOption("reason");
		if (targetOption == null || reasonOption == null) {
			return Responses.replyMissingArguments(event);
		}
		User target = targetOption.getAsUser();
		if (moderator.getIdLong() == target.getIdLong()) {
			return Responses.error(event, "You cannot perform this action on yourself.");
		}
		Objects.requireNonNull(event.getGuild()).retrieveMemberById(target.getIdLong()).queue(targetMember -> {
			if (targetMember.isOwner() || !moderator.canInteract(targetMember)) {
				Responses.error(event.getHook(), "You cannot perform actions on a higher member staff member.").queue();
				return;
			}
			WebhookMessageAction<Message> action = handleModerationUserCommand(event, moderator, target, reasonOption.getAsString());
			if (action != null) action.queue();
		}, e -> {
			WebhookMessageAction<Message> action = handleModerationUserCommand(event, moderator, target, reasonOption.getAsString());
			if (action != null) action.queue();
		});
		return event.deferReply();
	}

	protected abstract WebhookMessageAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason);
}
