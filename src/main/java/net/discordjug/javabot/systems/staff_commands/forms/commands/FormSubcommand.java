package net.discordjug.javabot.systems.staff_commands.forms.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * Base abstract class containing common methods used in form subcommands.
 */
public abstract class FormSubcommand extends Subcommand {

	private final BotConfig botConfig;

	/**
	 * The main constructor.
	 * 
	 * @param botConfig main bot configuration
	 */
	public FormSubcommand(BotConfig botConfig) {
		this.botConfig = botConfig;
	}

	/**
	 * Check if the author of this event has the configured staff role. If not,
	 * reply to the event with a message and return `false`
	 * 
	 * @param event event to reply to
	 * @return true if the user has the staff role
	 */
	protected boolean checkForStaffRole(IReplyCallback event) {
		if (!Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return false;
		}
		return true;
	}
}
