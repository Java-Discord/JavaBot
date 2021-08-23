package com.javadiscord.javabot.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

/**
 * Simple DTO representing an option that can be given to a Discord slash
 * command or subcommand.
 */
public record OptionConfig(String name, String description, String type, boolean required) {
	public OptionData toData() {
		return new OptionData(OptionType.valueOf(this.type.toUpperCase()), this.name, this.description, this.required);
	}

	public static OptionConfig fromData(OptionData data) {
		return new OptionConfig(data.getName(),
				data.getDescription(),
				data.getType().name(),
				data.isRequired());
	}
}
