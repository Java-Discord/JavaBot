package net.javadiscord.javabot.command.data.slash_commands;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Arrays;
import java.util.Objects;

/**
 * Simple DTO representing a top-level Discord slash command.
 */
@Data
public class SlashCommandConfig {
	private String name;
	private String description;
	private boolean enabledByDefault = true;
	private SlashCommandPrivilegeConfig[] privileges;
	private SlashOptionConfig[] options;
	private SlashSubCommandConfig[] subCommands;
	private SlashSubCommandGroupConfig[] subCommandGroups;
	private String handler;

	/**
	 * Creates a {@link SlashCommandConfig} object from the given {@link SlashCommandData}.
	 *
	 * @param data The original {@link SlashCommandData}.
	 * @return A new {@link SlashCommandConfig} object.
	 */
	public static SlashCommandConfig fromData(SlashCommandData data) {
		SlashCommandConfig c = new SlashCommandConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setOptions(data.getOptions().stream().map(SlashOptionConfig::fromData).toArray(SlashOptionConfig[]::new));
		c.setSubCommands(data.getSubcommands().stream().map(SlashSubCommandConfig::fromData).toArray(SlashSubCommandConfig[]::new));
		c.setSubCommandGroups(data.getSubcommandGroups().stream().map(SlashSubCommandGroupConfig::fromData).toArray(SlashSubCommandGroupConfig[]::new));
		c.setHandler(null);
		return c;
	}

	/**
	 * Converts the current {@link SlashCommandConfig} into a {@link SlashCommandData} object.
	 *
	 * @return The {@link SlashCommandData} object.
	 */
	public SlashCommandData toData() {
		SlashCommandData data = Commands.slash(this.name, this.description);
		data.setDefaultEnabled(this.enabledByDefault);
		if (this.options != null) {
			for (SlashOptionConfig option : this.options) {
				data.addOptions(option.toData());
			}
		}
		if (this.subCommands != null) {
			for (SlashSubCommandConfig subCommand : this.subCommands) {
				data.addSubcommands(subCommand.toData());
			}
		}
		if (this.subCommandGroups != null) {
			for (SlashSubCommandGroupConfig group : this.subCommandGroups) {
				data.addSubcommandGroups(group.toData());
			}
		}
		return data;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SlashCommandConfig that)) return false;
		return getName().equals(that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
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
}
