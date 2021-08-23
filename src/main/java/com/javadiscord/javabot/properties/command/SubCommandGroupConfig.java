package com.javadiscord.javabot.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.Arrays;
import java.util.List;

/**
 * Simple DTO for a group of Discord subcommands.
 */
public record SubCommandGroupConfig(String name, String description, List<SubCommandConfig> subCommands) {
	public SubcommandGroupData toData() {
		SubcommandGroupData data = new SubcommandGroupData(this.name, this.description);
		if (this.subCommands != null) {
			this.subCommands.stream().map(SubCommandConfig::toData).forEach(data::addSubcommands);
		}
		return data;
	}

	public static SubCommandGroupConfig fromData(SubcommandGroupData data) {
		return new SubCommandGroupConfig(data.getName(),
				data.getDescription(),
				data.getSubcommands().stream().map(SubCommandConfig::fromData).toList());
	}
}
