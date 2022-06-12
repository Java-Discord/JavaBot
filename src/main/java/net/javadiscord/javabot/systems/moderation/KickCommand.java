package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
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
 * Command that allows staff-members to kick members.
 */
public class KickCommand extends ModerateUserCommand {
	public KickCommand() {
		setSlashCommandData(Commands.slash("kick", "Kicks a member")
				.addOption(OptionType.USER, "user", "The user to kick.", true)
				.addOption(OptionType.STRING, "reason", "The reason for kicking this user.", true)
				.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the kick is issued.", false)
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull Member target, @Nullable String reason) {
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService moderationService = new ModerationService(event.getInteraction());
		moderationService.kick(target, reason, event.getMember(), event.getTextChannel(), quiet);
		return Responses.success(event, "User Kicked", String.format("%s has been kicked.", target.getAsMention()));
	}
}