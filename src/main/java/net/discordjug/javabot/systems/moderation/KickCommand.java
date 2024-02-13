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
 * <h3>This class represents the /kick command.</h3>
 */
public class KickCommand extends ModerateUserCommand {
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public KickCommand(BotConfig botConfig, ModerationService moderationService) {
		super(botConfig);
		this.moderationService = moderationService;
		setModerationSlashCommandData(Commands.slash("kick", "Kicks a member")
				.addOption(OptionType.USER, "user", "The user to kick.", true)
				.addOption(OptionType.STRING, "reason", "The reason for kicking this user.", true)
				.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the kick is issued.", false)
		);
	}

	@Override
	protected WebhookMessageCreateAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason) {
		if (!Checks.hasPermission(event.getGuild(), Permission.KICK_MEMBERS)) {
			return Responses.replyInsufficientPermissions(event.getHook(), Permission.KICK_MEMBERS);
		}
		boolean quiet = isQuiet(event);
		moderationService.kick(target, reason, event.getMember(), event.getChannel(), quiet);
		return Responses.success(event.getHook(), "User Kicked", "%s has been kicked.", target.getAsMention());
	}
}