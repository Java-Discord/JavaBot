package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An abstraction of {@link ModerateCommand} which is used for commands that require a user.
 */
public abstract class ModerateUserCommand extends ModerateCommand {
	public ModerateUserCommand(BotConfig botConfig) {
		super(botConfig);
	}

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
			if (isRequireStaff() && (targetMember.isOwner() || !moderator.canInteract(targetMember))) {
				Responses.error(event.getHook(), "You cannot perform actions on a higher staff member.").queue();
				return;
			}
			WebhookMessageCreateAction<Message> action = handleModerationUserCommand(event, moderator, target, reasonOption.getAsString());
			if (action != null) action.queue();
		}, e -> {
			WebhookMessageCreateAction<Message> action = handleModerationUserCommand(event, moderator, target, reasonOption.getAsString());
			if (action != null) action.queue();
		});
		return event.deferReply(true);
	}

	protected abstract WebhookMessageCreateAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason);

	/**
	 * Determines whether this command is executed quitely.
	 *
	 * If it is, no (public) response should be sent in the current channel. This does not effect logging.
	 *
	 * By default, moderative actions in the log channel are quiet.
	 * @param event The {@link SlashCommandInteractionEvent} corresponding to the executed command
	 * @return {@code true} iff the command is quiet, else {@code false}
	 */
	protected boolean isQuiet(SlashCommandInteractionEvent event) {
		return isQuiet(botConfig, event);
	}

	/**
	 * Determines whether this command is executed quitely.
	 *
	 * If it is, no (public) response should be sent in the current channel. This does not effect logging.
	 *
	 * By default, moderative actions in the log channel are quiet.
	 * @param botConfig the main configuration of the bot
	 * @param event The {@link SlashCommandInteractionEvent} corresponding to the executed command
	 * @return {@code true} iff the command is quiet, else {@code false}
	 */
	public static boolean isQuiet(BotConfig botConfig, SlashCommandInteractionEvent event) {
		return event.getOption("quiet",
				() -> event.getChannel().getIdLong() == botConfig.get(event.getGuild()).getModerationConfig().getLogChannelId(),
				OptionMapping::getAsBoolean);
	}
}
