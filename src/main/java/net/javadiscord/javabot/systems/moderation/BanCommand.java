package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Command that allows staff-members to ban guild members.
 */
public class BanCommand extends ModerateUserCommand {
	private final NotificationService notificationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param notificationService The {@link NotificationService}
	 */
	public BanCommand(NotificationService notificationService) {
		this.notificationService = notificationService;
		setModerationSlashCommandData(Commands.slash("ban", "Ban a user.")
						.addOption(OptionType.USER, "user", "The user to ban.", true)
						.addOption(OptionType.STRING, "reason", "The reason for banning this user.", true)
						.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the ban is issued.", false)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason) {
		if (!Checks.hasPermission(event.getGuild(), Permission.BAN_MEMBERS)) {
			return Responses.replyInsufficientPermissions(event.getHook(), Permission.BAN_MEMBERS);
		}
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService service = new ModerationService(notificationService, event.getInteraction());
		service.ban(target, reason, commandUser, event.getChannel(), quiet);
		return Responses.success(event.getHook(), "User Banned", "%s has been banned.", target.getAsMention());
	}
}