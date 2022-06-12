package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.moderation.ModerateUserCommand;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Command that allows staff-members to ban guild members.
 */
public class BanCommand extends ModerateUserCommand {
	public BanCommand() {
		setSlashCommandData(Commands.slash("ban", "Ban a user.")
						.addOption(OptionType.USER, "user", "The user to ban.", true)
						.addOption(OptionType.STRING, "reason", "The reason for banning this user.", true)
						.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the ban is issued.", false)
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		setRequireReason(true);
	}

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull Member target, @Nullable String reason) {
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService moderationService = new ModerationService(event.getInteraction());
		// TODO: Support non-guild users
		moderationService.ban(target.getUser(), reason, commandUser, event.getTextChannel(), quiet);
		return Responses.success(event, "User Banned", String.format("%s has been banned.", target.getAsMention()));
	}
}