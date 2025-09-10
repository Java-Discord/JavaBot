package net.discordjug.javabot.systems.staff_commands.forms.model;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

/**
 * Represents a form field.
 */
public class FormField {
	private final String label;
	private final int max;
	private final int min;
	private final String placeholder;
	private final boolean required;
	private final String style;
	private final String value;

	/**
	 * Main constructor.
	 *
	 * @param label       text field lable.
	 * @param max         maximum characters allowed.
	 * @param min         minimum characters allowed.
	 * @param placeholder text field placeholder.
	 * @param required    whether or not this field is required.
	 * @param style       text field style. One of {@link TextInputStyle} values.
	 *                    Case insensitive.
	 * @param value       initial value of this text field.
	 */
	public FormField(String label, int max, int min, String placeholder, boolean required, String style, String value) {
		this.label = label;
		this.max = max;
		this.min = min;
		this.placeholder = placeholder;
		this.required = required;
		this.style = style;
		this.value = value;
	}

	/**
	 * Create a standalone text input from this field.
	 *
	 * @param id ID of this text input.
	 * @return text input ready to use in a modal.
	 */
	public TextInput createTextInput(String id) {
		return TextInput.create(id, getLabel(), getStyle()).setRequiredRange(getMin(), getMax())
				.setPlaceholder(getPlaceholder()).setRequired(isRequired()).setValue(getValue()).build();
	}

	public String getLabel() {
		return label;
	}

	public int getMax() {
		return max <= 0 ? 64 : max;
	}

	public int getMin() {
		return Math.max(0, min);
	}

	public String getPlaceholder() {
		return placeholder;
	}

	/**
	 * Get a parsed style of this field's text input.
	 * 
	 * @return one of {@link TextInputStyle} values. Defaults to
	 *         {@link TextInputStyle#SHORT} if the stored value is invalid.
	 */
	public TextInputStyle getStyle() {
		try {
			return style == null ? TextInputStyle.SHORT : TextInputStyle.valueOf(style.toUpperCase());
		} catch (IllegalArgumentException e) {
			return TextInputStyle.SHORT;
		}
	}

	public String getValue() {
		return value;
	}

	public boolean isRequired() {
		return required;
	}

	@Override
	public String toString() {
		return "FormField [label=" + label + ", max=" + max + ", min=" + min + ", placeholder=" + placeholder
				+ ", required=" + required + ", style=" + style + ", value=" + value + "]";
	}

}
