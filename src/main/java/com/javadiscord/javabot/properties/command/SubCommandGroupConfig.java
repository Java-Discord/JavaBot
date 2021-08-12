package com.javadiscord.javabot.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.Arrays;

/**
 * Simple DTO for a group of Discord subcommands.
 */
@Data
public class SubCommandGroupConfig {
	private String name;
	private String description;
	private SubCommandConfig[] subCommands;

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
