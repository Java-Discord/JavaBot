package com.javadiscord.javabot.properties.command;

import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.Arrays;

public class SubCommandGroupConfig {
	private String name;
	private String description;
	private SubCommandConfig[] subCommands;

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

	public SubCommandConfig[] getSubCommands() {
		return subCommands;
	}

	public void setSubCommands(SubCommandConfig[] subCommands) {
		this.subCommands = subCommands;
	}

	public SubcommandGroupData toData() {
		SubcommandGroupData data = new SubcommandGroupData(this.name, this.description);
		if (this.subCommands != null) {
			for (SubCommandConfig scc : this.subCommands) {
				data.addSubcommands(scc.toData());
			}
		}
		return data;
	}

	@Override
	public String toString() {
		return "SubCommandGroupConfig{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", subCommands=" + Arrays.toString(subCommands) +
			'}';
	}

	public static SubCommandGroupConfig fromData(SubcommandGroupData data) {
		SubCommandGroupConfig c = new SubCommandGroupConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setSubCommands(data.getSubcommands().stream().map(SubCommandConfig::fromData).toArray(SubCommandConfig[]::new));
		return c;
	}
}
