package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Arrays;
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
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form add-field` command.
 */
public class AddFieldFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public AddFieldFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("add-field", "Adds a field to an existing form")
				.addOption(OptionType.INTEGER, "form-id", "Form ID to add the field to", true, true)
				.addOption(OptionType.STRING, "label", "Field label", true)
				.addOption(OptionType.INTEGER, "min", "Minimum number of characters")
				.addOption(OptionType.INTEGER, "max", "Maximum number of characters")
				.addOption(OptionType.STRING, "placeholder", "Field placeholder")
				.addOption(OptionType.BOOLEAN, "required",
						"Whether or not the user has to input data in this field. Default: false")
				.addOption(OptionType.STRING, "style", "Input style. Default: SHORT", false, true)
				.addOption(OptionType.STRING, "value", "Initial field value")
				.addOption(OptionType.INTEGER, "index", "Index to insert the field at"));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();

		if (form.getFields().size() >= 5) {
			event.getHook().sendMessage("Can't add more than 5 components to a form").queue();
			return;
		}

		int index = event.getOption("index", -1, OptionMapping::getAsInt);
		if (index < -1 || index >= form.getFields().size()) {
			event.getHook().sendMessage("Field index out of bounds").queue();
			return;
		}

		formsRepo.addField(form, createFormFieldFromEvent(event), index);
		event.getHook().sendMessage("Added a new field to the form.").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		switch (target.getName()) {
			case "form-id" -> event.replyChoices(
					formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
					.queue();
			case "style" ->
				event.replyChoices(Arrays.stream(TextInputStyle.values()).filter(t -> t != TextInputStyle.UNKNOWN)
						.map(style -> new Choice(style.name(), style.name())).toList()).queue();
			default -> {}
		}
	}

	private static FormField createFormFieldFromEvent(SlashCommandInteractionEvent e) {
		String label = e.getOption("label", OptionMapping::getAsString);
		int min = e.getOption("min", 0, OptionMapping::getAsInt);
		int max = e.getOption("max", 64, OptionMapping::getAsInt);
		String placeholder = e.getOption("placeholder", OptionMapping::getAsString);
		boolean required = e.getOption("required", false, OptionMapping::getAsBoolean);
		TextInputStyle style = e.getOption("style", TextInputStyle.SHORT, t -> {
			try {
				return TextInputStyle.valueOf(t.getAsString().toUpperCase());
			} catch (IllegalArgumentException e2) {
				return TextInputStyle.SHORT;
			}
		});
		String value = e.getOption("value", OptionMapping::getAsString);

		return new FormField(label, max, min, placeholder, required, style.name(), value);
	}
}
