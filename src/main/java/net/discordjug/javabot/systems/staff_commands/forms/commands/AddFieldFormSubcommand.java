package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Arrays;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormField;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form add-field` command. This command allows for modification of
 * {@link FormData} by adding new fields to it. See
 * {@link RemoveFieldFormSubcommand} for the command used to remove fields from
 * a form.<br>
 * Currently, due to Discord limitations, only 5 fields are allowed per form.
 * Trying to add more fields will have no effect.
 * 
 * @see FormData
 */
public class AddFieldFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public AddFieldFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("add-field", "Adds a field to an existing form")
				.addOption(OptionType.INTEGER, FORM_ID_FIELD, "Form ID to add the field to", true, true)
				.addOption(OptionType.STRING, "label", "Field label", true)
				.addOption(OptionType.INTEGER, "min", "Minimum number of characters")
				.addOption(OptionType.INTEGER, "max", "Maximum number of characters")
				.addOption(OptionType.STRING, "placeholder", "Field placeholder")
				.addOption(OptionType.BOOLEAN, "required",
						"Whether or not the user has to input data in this field. Default: false")
				.addOption(OptionType.STRING, "style", "Input style. Default: SHORT", false, true)
				.addOption(OptionType.STRING, "value", "Initial field value"));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();

		if (form.fields().size() >= 5) {
			event.getHook().sendMessage("Can't add more than 5 components to a form").queue();
			return;
		}

		formsRepo.addField(form, createFormFieldFromEvent(event));
		event.getHook().sendMessage("Added a new field to the form.").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		if (!handleFormIDAutocomplete(event, target) && "style".equals(target.getName())) {
			event.replyChoices(Arrays.stream(TextInputStyle.values()).filter(t -> t != TextInputStyle.UNKNOWN)
					.map(style -> new Choice(style.name(), style.name())).toList()).queue();
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

		return new FormField(label, max, min, placeholder, required, style, value, 0);
	}
}
