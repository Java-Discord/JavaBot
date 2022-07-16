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
 * <h3>This class represents the /kick command.</h3>
 */
public class KickCommand extends ModerateUserCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public KickCommand() {
		setSlashCommandData(Commands.slash("kick", "Kicks a member")
				.addOption(OptionType.USER, "user", "The user to kick.", true)
				.addOption(OptionType.STRING, "reason", "The reason for kicking this user.", true)
				.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the kick is issued.", false)
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nullable String reason) {
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService moderationService = new ModerationService(event.getInteraction());
		moderationService.kick(target, reason, event.getMember(), event.getTextChannel(), quiet);
		return Responses.success(event, "User Kicked", String.format("%s has been kicked.", target.getAsMention()));
	}
}