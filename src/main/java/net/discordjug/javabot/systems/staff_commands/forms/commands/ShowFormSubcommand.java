package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form show` command. Brings up an input modal for the given form. This
 * command works even if the form is currently not accepting new submissions
 * (due to being closed or expired), or is not attached to a message.
 * 
 * @see FormData
 */
public class ShowFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public ShowFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("show",
				"Forcefully opens a form dialog, even if it's closed, or not attached to a message")
				.addOption(OptionType.INTEGER, FORM_ID_FIELD, "Form ID to add the field to", true, true));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			Responses.error(event, "A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();
		if (form.fields().isEmpty()) {
			Responses.error(event, "You can't open a form with no fields").queue();
			return;
		}
		event.replyModal(FormInteractionManager.createSubmissionModal(form)).queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}
}
