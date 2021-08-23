package com.javadiscord.javabot.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

/**
 * Simple DTO for a Discord subcommand.
 */
public record SubCommandConfig(String name, String description, List<OptionConfig> options) {
	public SubcommandData toData() {
		SubcommandData data = new SubcommandData(this.name, this.description);
		if (this.options != null) {
			this.options.stream().map(OptionConfig::toData).forEach(data::addOptions);
		}
		return data;
	}

	public static SubCommandConfig fromData(SubcommandData data) {
		return new SubCommandConfig(data.getName(),
				data.getDescription(),
				data.getOptions().stream().map(OptionConfig::fromData).toList());
	}
}
