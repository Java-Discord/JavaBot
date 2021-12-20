package com.javadiscord.javabot.data.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

/**
 * DTO for a choice that a slash command option can have.
 */
@Data
public class OptionChoiceConfig {
	private String name;
	private String value;

	/**
	 * Converts this choice data into a JDA object for use with the API.
	 * @return The JDA option choice object.
	 */
	public Choice toData() {
		return new Choice(name, value);
	}

	public static OptionChoiceConfig fromData(Choice choice) {
		var c = new OptionChoiceConfig();
		c.setName(choice.getName());
		c.setValue(choice.getAsString());
		return c;
	}
}
