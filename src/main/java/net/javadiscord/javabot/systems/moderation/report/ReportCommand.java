package net.javadiscord.javabot.systems.moderation.report;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.systems.moderation.ModerateUserCommand;

import javax.annotation.Nonnull;

/**
 * <h3>This class represents the /report command.</h3>
 */
@Slf4j
public class ReportCommand extends ModerateUserCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public ReportCommand() {
		setSlashCommandData(Commands.slash("report", "Reports a member.")
				.addOption(OptionType.USER, "user", "The user you want to report", true)
				.addOption(OptionType.STRING, "reason", "The reason", true)
				.setGuildOnly(true)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nonnull String reason) {
		return new ReportManager().handleUserReport(event.getHook(), reason, target.getId());
	}
}


