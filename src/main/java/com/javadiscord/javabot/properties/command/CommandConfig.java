package com.javadiscord.javabot.properties.command;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

/**
 * Simple DTO representing a top-level Discord slash command.
 */
public record CommandConfig(String name, String description, boolean enabledByDefault, List<CommandPrivilegeConfig> privileges, List<OptionConfig> options, List<SubCommandConfig> subCommands, List<SubCommandGroupConfig> subCommandGroups, String handler) {

	public CommandData toData() {
		CommandData data = new CommandData(this.name, this.description);
		data.setDefaultEnabled(this.enabledByDefault);
		if (this.options != null) {
			this.options.stream().map(OptionConfig::toData).forEach(data::addOptions);
		}
		if (this.subCommands != null) {
			this.subCommands.stream().map(SubCommandConfig::toData).forEach(data::addSubcommands);
		}
		if (this.subCommandGroups != null) {
			this.subCommandGroups.stream().map(SubCommandGroupConfig::toData).forEach(data::addSubcommandGroups);
		}
		return data;
	}

	public static CommandConfig fromData(CommandData data) {
		return new CommandConfig(data.getName(),
				data.getDescription(),
				true,
				null,
				data.getOptions().stream().map(OptionConfig::fromData).toList(),
				data.getSubcommands().stream().map(SubCommandConfig::fromData).toList(),
				data.getSubcommandGroups().stream().map(SubCommandGroupConfig::fromData).toList(),
				null);
	}
}
