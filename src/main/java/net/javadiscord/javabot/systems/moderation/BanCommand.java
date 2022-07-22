package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Command that allows staff-members to ban guild members.
 */
public class BanCommand extends ModerateUserCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public BanCommand() {
		setSlashCommandData(Commands.slash("ban", "Ban a user.")
						.addOption(OptionType.USER, "user", "The user to ban.", true)
						.addOption(OptionType.STRING, "reason", "The reason for banning this user.", true)
						.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the ban is issued.", false)
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		setRequireReason(true);
	}

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason) {
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService service = new ModerationService(event.getInteraction());
		service.ban(target, reason, commandUser, event.getChannel(), quiet);
		return Responses.success(event, "User Banned", "%s has been banned.", target.getAsMention());
	}
}