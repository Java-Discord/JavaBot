package net.javadiscord.javabot.command.data.slash_commands;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Arrays;

/**
 * Simple DTO representing an option that can be given to a Discord slash
 * command or subcommand.
 */
@Data
public class SlashOptionConfig {
	private String name;
	private String description;
	private String type;
	private boolean required;
	private boolean autocomplete = false;
	private SlashOptionChoiceConfig[] choices;

	/**
	 * Converts the given {@link OptionData} into a {@link SlashOptionConfig} object.
	 *
	 * @param data The {@link OptionData}.
	 * @return The {@link SlashOptionConfig} object.
	 */
	public static SlashOptionConfig fromData(OptionData data) {
		SlashOptionConfig c = new SlashOptionConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setType(data.getType().name());
		c.setRequired(data.isRequired());
		c.setAutocomplete(data.isAutoComplete());
		c.setChoices(data.getChoices().stream().map(SlashOptionChoiceConfig::fromData).toArray(SlashOptionChoiceConfig[]::new));
		return c;
	}

	/**
	 * Converts the current {@link SlashOptionConfig} to a {@link OptionData} object.
	 *
	 * @return The {@link OptionData} object.
	 */
	public OptionData toData() {
		var d = new OptionData(OptionType.valueOf(this.type.toUpperCase()), this.name, this.description, this.required, this.autocomplete);
		if (this.choices != null && this.choices.length > 0) {
			d.addChoices(Arrays.stream(this.choices).map(SlashOptionChoiceConfig::toData).toList());
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
				", autocomplete=" + autocomplete +
				", choices=" + Arrays.toString(choices) +
				'}';
	}
}
