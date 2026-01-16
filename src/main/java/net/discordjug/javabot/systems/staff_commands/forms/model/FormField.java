package net.discordjug.javabot.systems.staff_commands.forms.model;

import java.util.Objects;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

/**
 * Represents a form field. Form fields are used to store data about text inputs
 * presented to the user in the form modal. Each form can have up to 5 fields.
 * 
 * @param label       field label.
 * @param max         maximum number of characters allowed to be entered in this
 *                    field.
 * @param min         minimum number of characters required. Setting min to a
 *                    value greater than 0 will make this field effectively
 *                    required, even if the {@code required} parameter is set to
 *                    false.
 * @param placeholder field placeholder. Use null to use any placeholder.
 * @param required    whether or not the user has to type something in this
 *                    field.
 * @param style       text input style.
 * @param value       initial field value. Can be null to indicate no inital
 *                    value.
 * @param id          form id.
 */
public record FormField(String label, int max, int min, String placeholder, boolean required, TextInputStyle style,
		String value, long id) {

	/**
	 * The main constructor.
	 */
	public FormField {
		Objects.requireNonNull(label);
		if (min < 0) throw new IllegalArgumentException("min < 0");

		if (max < 1) throw new IllegalArgumentException("max < 1");

		if (max < min) throw new IllegalArgumentException("max < min");

		Objects.requireNonNull(style);
	}

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
