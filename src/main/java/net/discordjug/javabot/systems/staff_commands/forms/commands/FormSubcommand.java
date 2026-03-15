package net.discordjug.javabot.systems.staff_commands.forms.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * Base abstract class containing common methods used in form subcommands.
 */
public abstract class FormSubcommand extends Subcommand {

	/**
	 * Form ID field identificator used in form subcommands.
	 */
	protected static final String FORM_ID_FIELD = "form-id";
	private final BotConfig botConfig;
	private final FormsRepository formsRepository;

	/**
	 * The main constructor.
	 * 
	 * @param botConfig main bot configuration
	 * @param formsRepository the forms repository
	 */
	public FormSubcommand(BotConfig botConfig, FormsRepository formsRepository) {
		this.botConfig = botConfig;
		this.formsRepository = formsRepository;
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

	/**
	 * Tries to handle the auto completion event initiated by a user. If current
	 * focused field's id is equal to {@link #FORM_ID_FIELD}, the method will handle
	 * the event by replying with a list of all available form IDs,
	 * 
	 * @param event  the event to handle
	 * @param target auto completion target
	 * @return true if the event was handled by this method
	 */
	protected boolean handleFormIDAutocomplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		if (FORM_ID_FIELD.equals(target.getName())) {
			event.replyChoices(
					formsRepository.getAllForms().stream().map(form -> new Choice(form.toString(), form.id())).toList())
					.queue();
			return true;
		}
		return false;
	}
}
