package com.javadiscord.javabot.properties.command;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

/**
 * Simple DTO representing an option that can be given to a Discord slash
 * command or subcommand.
 */
public class OptionConfig {
	private String name;
	private String description;
	private String type;
	private boolean required;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

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
