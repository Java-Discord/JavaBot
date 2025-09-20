package net.discordjug.javabot.systems.staff_commands.forms.model;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

/**
 * Represents a form field.
 * Form fields are used to store data about text inputs presented to the user in the form modal.
 * Each form can have up to 5 fields.
 */
public record FormField(String label, int max, int min, String placeholder, boolean required,
		TextInputStyle style, String value, long id) {

	/**
	 * Create a text input from this field.
	 *
	 * @param id ID of this text input.
	 * @return text input ready to use in a modal.
	 */
	public TextInput createTextInput(String id) {
		return TextInput.create(id, label(), style()).setRequiredRange(min(), max()).setPlaceholder(placeholder())
				.setRequired(required()).setValue(value()).build();
	}
}
