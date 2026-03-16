package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormField;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;

/**
 * The `/form remove-field` command. This command removes a field from the form.
 *
 * @see AddFieldFormSubcommand
 * @see FormData
 */
public class RemoveFieldFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public RemoveFieldFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("remove-field", "Removse a field from an existing form")
				.addOption(OptionType.INTEGER, FORM_ID_FIELD, "Form ID to add the field to", true, true)
				.addOption(OptionType.INTEGER, "field", "0-indexed # of the field to remove", true, true));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		int index = event.getOption("field", OptionMapping::getAsInt);
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();

		if (form.isAttached() && form.fields().size() <= 1) {
			event.getHook().sendMessage(
					"Can't remove the last field from an attached form. Detach the form before removing the field")
					.queue();
			return;
		}

		if (!formsRepo.removeField(form, index)) {
			event.getHook().sendMessage("A field on this index was not found.").queue();
			return;
		}

		event.getHook().sendMessage("Removed field `" + form.fields().get(index).label() + "` from the form.").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		if (!handleFormIDAutocomplete(event, target) && "field".equals(target.getName())) {
			Long formId = event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong);
			if (formId != null) {
				Optional<FormData> form = formsRepo.getForm(formId);
				if (form.isPresent()) {
					List<Choice> choices = new ArrayList<>();
					List<FormField> fields = form.get().fields();
					for (int i = 0; i < fields.size(); i++) {
						choices.add(new Choice(fields.get(i).label(), i));
					}
					event.replyChoices(AutoCompleteUtils.filterChoices(event, choices)).queue();
					return;
				}
			}
		}
	}
}
