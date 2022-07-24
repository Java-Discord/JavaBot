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
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <h3>This class represents the /kick command.</h3>
 */
public class KickCommand extends ModerateUserCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public KickCommand() {
		setModerationSlashCommandData(Commands.slash("kick", "Kicks a member")
				.addOption(OptionType.USER, "user", "The user to kick.", true)
				.addOption(OptionType.STRING, "reason", "The reason for kicking this user.", true)
				.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the kick is issued.", false)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason) {
		if (!Checks.hasPermission(event.getGuild(), Permission.KICK_MEMBERS)) {
			return Responses.replyInsufficientPermissions(event.getHook(), Permission.KICK_MEMBERS);
		}
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService service = new ModerationService(event.getInteraction());
		service.kick(target, reason, event.getMember(), event.getChannel(), quiet);
		return Responses.success(event.getHook(), "User Kicked", "%s has been kicked.", target.getAsMention());
	}
}