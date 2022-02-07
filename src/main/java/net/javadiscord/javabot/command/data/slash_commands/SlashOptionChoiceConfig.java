package net.javadiscord.javabot.command.data.slash_commands;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

/**
 * DTO for a choice that a slash command option can have.
 */
@Data
public class SlashOptionChoiceConfig {
	private String name;
	private String value;

	/**
	 * Converts the given {@link Choice} into a {@link SlashOptionChoiceConfig} object.
	 *
	 * @param choice The {@link Choice}.
	 * @return The {@link SlashOptionChoiceConfig} object.
	 */
	public static SlashOptionChoiceConfig fromData(Choice choice) {
		var c = new SlashOptionChoiceConfig();
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
