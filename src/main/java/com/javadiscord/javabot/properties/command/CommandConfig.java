package com.javadiscord.javabot.properties.command;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Arrays;

/**
 * Simple DTO representing a top-level Discord slash command.
 */
public class CommandConfig {
	private String name;
	private String description;
	private OptionConfig[] options;
	private SubCommandConfig[] subCommands;
	private SubCommandGroupConfig[] subCommandGroups;
	private String handler;

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

	public OptionConfig[] getOptions() {
		return options;
	}

	public void setOptions(OptionConfig[] options) {
		this.options = options;
	}

	public SubCommandConfig[] getSubCommands() {
		return subCommands;
	}

	public void setSubCommands(SubCommandConfig[] subCommands) {
		this.subCommands = subCommands;
	}

	public SubCommandGroupConfig[] getSubCommandGroups() {
		return subCommandGroups;
	}

	public void setSubCommandGroups(SubCommandGroupConfig[] subCommandGroups) {
		this.subCommandGroups = subCommandGroups;
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public CommandData toData() {
		CommandData data = new CommandData(this.name, this.description);
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
