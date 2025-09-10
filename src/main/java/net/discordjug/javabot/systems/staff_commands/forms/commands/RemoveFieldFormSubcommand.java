package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form remove-field` command.
 */
public class RemoveFieldFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public RemoveFieldFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("remove-field", "Removse a field from an existing form")
				.addOption(OptionType.INTEGER, "form-id", "Form ID to add the field to", true, true)
				.addOption(OptionType.INTEGER, "field", "# of the field to remove", true, true));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		int index = event.getOption("field", OptionMapping::getAsInt);
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();
		if (index < 0 || index >= form.getFields().size()) {
			event.getHook().sendMessage("Field index out of bounds.").queue();
			return;
		}

		if (form.getMessageChannel() != null && form.getMessageId() != null && form.getFields().size() <= 1) {
			event.getHook().sendMessage(
					"Can't remove the last field from an attached form. Detach the form before removing the field")
					.queue();
			return;
		}

		formsRepo.removeField(form, index);

		event.getHook().sendMessage("Removed field `" + form.getFields().get(index).getLabel() + "` from the form.")
				.queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		switch (target.getName()) {
			case "form-id" -> event.replyChoices(
					formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
					.queue();
			case "field" -> {
				Long formId = event.getOption("form-id", OptionMapping::getAsLong);
				if (formId != null) {
					Optional<FormData> form = formsRepo.getForm(formId);
					if (form.isPresent()) {
						List<Choice> choices = new ArrayList<>();
						List<FormField> fields = form.get().getFields();
						for (int i = 0; i < fields.size(); i++) {
							choices.add(new Choice(fields.get(i).getLabel(), i));
						}
						event.replyChoices(choices).queue();
						return;
					}
				}
				event.replyChoices().queue();
			}
			default -> {}
		}
	}
}
