package net.javadiscord.javabot.command.data.slash_commands;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.Arrays;

/**
 * Simple DTO for a group of Discord subcommands.
 */
@Data
public class SlashSubCommandGroupConfig {
	private String name;
	private String description;
	private SlashSubCommandConfig[] subCommands;

	/**
	 * Converts the given {@link SubcommandGroupData} into a {@link SlashSubCommandGroupConfig} object.
	 *
	 * @param data The {@link SubcommandGroupData}.
	 * @return The {@link SlashSubCommandGroupConfig} object.
	 */
	public static SlashSubCommandGroupConfig fromData(SubcommandGroupData data) {
		SlashSubCommandGroupConfig c = new SlashSubCommandGroupConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setSubCommands(data.getSubcommands().stream().map(SlashSubCommandConfig::fromData).toArray(SlashSubCommandConfig[]::new));
		return c;
	}

	/**
	 * Converts the current {@link SlashSubCommandGroupConfig} into a {@link SubcommandGroupData} object.
	 *
	 * @return The {@link SubcommandGroupData} object.
	 */
	public SubcommandGroupData toData() {
		SubcommandGroupData data = new SubcommandGroupData(this.name, this.description);
		if (this.subCommands != null) {
			for (SlashSubCommandConfig scc : this.subCommands) {
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
}
