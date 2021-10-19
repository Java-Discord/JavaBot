package com.javadiscord.javabot.data.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Arrays;

/**
 * Simple DTO representing a top-level Discord slash command.
 */
@Data
public class CommandConfig {
	private String name;
	private String description;
	private boolean enabledByDefault = true;
	private CommandPrivilegeConfig[] privileges;
	private OptionConfig[] options;
	private SubCommandConfig[] subCommands;
	private SubCommandGroupConfig[] subCommandGroups;
	private String handler;

	public CommandData toData() {
		CommandData data = new CommandData(this.name, this.description);
		data.setDefaultEnabled(this.enabledByDefault);
		if (this.options != null) {
			for (OptionConfig option : this.options) {
				data.addOptions(option.toData());
			}
		}
		if (this.subCommands != null) {
			for (SubCommandConfig subCommand : this.subCommands) {
				data.addSubcommands(subCommand.toData());
			}
		}
		if (this.subCommandGroups != null) {
			for (SubCommandGroupConfig group : this.subCommandGroups) {
				data.addSubcommandGroups(group.toData());
			}
		}
		return data;
	}

	@Override
	public String toString() {
		return "CommandConfig{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", options=" + Arrays.toString(options) +
			", subCommands=" + Arrays.toString(subCommands) +
			", subCommandGroups=" + Arrays.toString(subCommandGroups) +
			", handler=" + handler +
			'}';
	}

	public static CommandConfig fromData(CommandData data) {
		CommandConfig c = new CommandConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setOptions(data.getOptions().stream().map(OptionConfig::fromData).toArray(OptionConfig[]::new));
		c.setSubCommands(data.getSubcommands().stream().map(SubCommandConfig::fromData).toArray(SubCommandConfig[]::new));
		c.setSubCommandGroups(data.getSubcommandGroups().stream().map(SubCommandGroupConfig::fromData).toArray(SubCommandGroupConfig[]::new));
		c.setHandler(null);
		return c;
	}
}
