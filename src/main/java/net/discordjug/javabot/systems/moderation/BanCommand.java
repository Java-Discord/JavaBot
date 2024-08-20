package net.discordjug.javabot.systems.moderation;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Command that allows staff-members to ban guild members.
 */
public class BanCommand extends ModerateUserCommand {
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public BanCommand(BotConfig botConfig, ModerationService moderationService) {
		super(botConfig);
		this.moderationService = moderationService;
		setModerationSlashCommandData(Commands.slash("ban", "Ban a user.")
						.addOption(OptionType.USER, "user", "The user to ban.", true)
						.addOption(OptionType.STRING, "reason", "The reason for banning this user.", true)
						.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the ban is issued.", false)
		);
	}

	@Override
	protected WebhookMessageCreateAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason) {
		if (!Checks.hasPermission(event.getGuild(), Permission.BAN_MEMBERS)) {
			return Responses.replyInsufficientPermissions(event.getHook(), Permission.BAN_MEMBERS);
		}
		boolean quiet = isQuiet(event);
		moderationService.ban(target, reason, commandUser, event.getChannel(), quiet);
		return Responses.success(event.getHook(), "User Banned", "%s has been banned.", target.getAsMention());
	}
}