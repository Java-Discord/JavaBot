package net.javadiscord.javabot.systems.moderation.report;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.moderation.ModerateUserCommand;

import javax.annotation.Nonnull;

/**
 * Command that allows members to report other members.
 */
@Slf4j
public class ReportCommand extends ModerateUserCommand {
	public ReportCommand() {
		setSlashCommandData(Commands.slash("report", "Reports a member.")
				.addOption(OptionType.USER, "user", "The user you want to report", true)
				.addOption(OptionType.STRING, "reason", "The reason", true)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationActionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull Member target, @Nonnull String reason) {
		new ReportManager(event.getUser()).handleUserReport(event.getHook(), reason, target.getId());
		return event.deferReply(true);
	}
}


