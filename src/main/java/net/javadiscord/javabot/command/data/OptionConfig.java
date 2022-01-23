package net.javadiscord.javabot.command.data;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Arrays;

/**
 * Simple DTO representing an option that can be given to a Discord slash
 * command or subcommand.
 */
@Data
public class OptionConfig {
	private String name;
	private String description;
	private String type;
	private boolean required;
	private OptionChoiceConfig[] choices;

	/**
	 * Converts the given {@link OptionData} into a {@link OptionConfig} object.
	 *
	 * @param data The {@link OptionData}.
	 * @return The {@link OptionConfig} object.
	 */
	public static OptionConfig fromData(OptionData data) {
		OptionConfig c = new OptionConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setType(data.getType().name());
		c.setRequired(data.isRequired());
		c.setChoices(data.getChoices().stream().map(OptionChoiceConfig::fromData).toArray(OptionChoiceConfig[]::new));
		return c;
	}

	/**
	 * Converts the current {@link OptionConfig} to a {@link OptionData} object.
	 * @return The {@link OptionData} object.
	 */
	public OptionData toData() {
		var d = new OptionData(OptionType.valueOf(this.type.toUpperCase()), this.name, this.description, this.required);
		if (this.choices != null && this.choices.length > 0) {
			d.addChoices(Arrays.stream(this.choices).map(OptionChoiceConfig::toData).toList());
		}
		return d;
	}

	@Override
	public String toString() {
		return "OptionConfig{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", type='" + type + '\'' +
				", required=" + required +
				", choices=" + Arrays.toString(choices) +
				'}';
	}
}
