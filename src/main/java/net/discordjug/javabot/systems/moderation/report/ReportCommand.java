package net.discordjug.javabot.systems.moderation.report;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerateUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

import javax.annotation.Nonnull;

/**
 * <h3>This class represents the /report command.</h3>
 */
public class ReportCommand extends ModerateUserCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public ReportCommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(Commands.slash("report", "Reports a member.")
				.addOption(OptionType.USER, "user", "The user you want to report", true)
				.addOption(OptionType.STRING, "reason", "The reason", true)
				.setGuildOnly(true)
		);
		setRequireStaff(false);
	}

	@Override
	protected WebhookMessageCreateAction<Message> handleModerationUserCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Member commandUser, @Nonnull User target, @Nonnull String reason) {
		return new ReportManager(botConfig).handleUserReport(event.getHook(), reason, target.getId());
	}
}


