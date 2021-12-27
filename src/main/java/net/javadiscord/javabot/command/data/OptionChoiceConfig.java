package net.javadiscord.javabot.command.data;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

/**
 * DTO for a choice that a slash command option can have.
 */
@Data
public class OptionChoiceConfig {
	private String name;
	private String value;

	public static OptionChoiceConfig fromData(Choice choice) {
		var c = new OptionChoiceConfig();
		c.setName(choice.getName());
		c.setValue(choice.getAsString());
		return c;
	}

	/**
	 * Converts this choice data into a JDA object for use with the API.
	 *
	 * @return The JDA option choice object.
	 */
	public Choice toData() {
		return new Choice(name, value);
	}

	@Override
	public String toString() {
		return "OptionChoiceConfig{" +
				"name='" + name + '\'' +
				", value='" + value + '\'' +
				'}';
	}
}
