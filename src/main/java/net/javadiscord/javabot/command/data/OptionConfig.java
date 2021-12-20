package net.javadiscord.javabot.command.data;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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

	public OptionData toData() {
		return new OptionData(OptionType.valueOf(this.type.toUpperCase()), this.name, this.description, this.required);
	}

	@Override
	public String toString() {
		return "OptionConfig{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", type='" + type + '\'' +
			", required=" + required +
			'}';
	}

	public static OptionConfig fromData(OptionData data) {
		OptionConfig c = new OptionConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setType(data.getType().name());
		c.setRequired(data.isRequired());
		return c;
	}
}
