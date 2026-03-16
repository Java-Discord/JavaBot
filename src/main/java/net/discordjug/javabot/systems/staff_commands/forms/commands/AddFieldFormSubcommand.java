package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Arrays;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormField;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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

	private static final String FORM_VALUE_FIELD = "value";
	private static final String FORM_STYLE_FIELD = "style";
	private static final String FORM_REQUIRED_FIELD = "required";
	private static final String FORM_PLACEHOLDER_FIELD = "placeholder";
	private static final String FORM_MAX_FIELD = "max";
	private static final String FORM_MIN_FIELD = "min";
	private static final String FORM_LABEL_FIELD = "label";
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
				.addOption(OptionType.STRING, FORM_LABEL_FIELD, "Field label", true)
				.addOption(OptionType.INTEGER, FORM_MIN_FIELD, "Minimum number of characters")
				.addOption(OptionType.INTEGER, FORM_MAX_FIELD, "Maximum number of characters")
				.addOption(OptionType.STRING, FORM_PLACEHOLDER_FIELD, "Field placeholder")
				.addOption(OptionType.BOOLEAN, FORM_REQUIRED_FIELD,
						"Whether or not the user has to input data in this field. Default: false")
				.addOptions(
						new OptionData(OptionType.STRING, FORM_STYLE_FIELD, "Input style. Default: SHORT", false)
								.addChoices(
										Arrays.stream(TextInputStyle.values()).filter(t -> t != TextInputStyle.UNKNOWN)
												.map(style -> new Choice(style.name(), style.name())).toList()))
				.addOption(OptionType.STRING, FORM_VALUE_FIELD, "Initial field value"));
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

		if (form.fields().size() >= Message.MAX_COMPONENT_COUNT) {
			event.getHook().sendMessage("Can't add more than 5 components to a form").queue();
			return;
		}

		formsRepo.addField(form, createFormFieldFromEvent(event));
		event.getHook().sendMessage("Added a new field to the form.").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}

	private static FormField createFormFieldFromEvent(SlashCommandInteractionEvent e) {
		String label = e.getOption(FORM_LABEL_FIELD, OptionMapping::getAsString);
		int min = e.getOption(FORM_MIN_FIELD, 0, OptionMapping::getAsInt);
		int max = e.getOption(FORM_MAX_FIELD, 64, OptionMapping::getAsInt);
		String placeholder = e.getOption(FORM_PLACEHOLDER_FIELD, OptionMapping::getAsString);
		boolean required = e.getOption(FORM_REQUIRED_FIELD, false, OptionMapping::getAsBoolean);
		TextInputStyle style = e.getOption(FORM_STYLE_FIELD, TextInputStyle.SHORT, t -> {
			try {
				return TextInputStyle.valueOf(t.getAsString().toUpperCase());
			} catch (IllegalArgumentException e2) {
				return TextInputStyle.SHORT;
			}
		});
		String value = e.getOption(FORM_VALUE_FIELD, OptionMapping::getAsString);

		return new FormField(label, max, min, placeholder, required, style, value, 0);
	}
}
