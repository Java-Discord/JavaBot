package net.discordjug.javabot.systems.staff_commands.forms.model;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

/**
 * Represents a form field.
 */
public record FormField(String label, int max, int min, String placeholder, boolean required,
		TextInputStyle style, String value, long id) {

	/**
	 * Create a standalone text input from this field.
	 *
	 * @param id ID of this text input.
	 * @return text input ready to use in a modal.
	 */
	public TextInput createTextInput(String id) {
		return TextInput.create(id, label(), style()).setRequiredRange(min(), max()).setPlaceholder(placeholder())
				.setRequired(required()).setValue(value()).build();
	}

	@Override
	public String toString() {
		return "FormField [label=" + label + ", max=" + max + ", min=" + min + ", placeholder=" + placeholder
				+ ", required=" + required + ", style=" + style + ", value=" + value + "]";
	}

}
