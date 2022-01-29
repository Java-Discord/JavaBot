package net.javadiscord.javabot.command.data;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Arrays;
import java.util.Objects;

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

	/**
	 * Creates a {@link CommandConfig} object from the given {@link SlashCommandData}.
	 *
	 * @param data The original {@link SlashCommandData}.
	 * @return A new {@link CommandConfig} object.
	 */
	public static CommandConfig fromData(SlashCommandData data) {
		CommandConfig c = new CommandConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setOptions(data.getOptions().stream().map(OptionConfig::fromData).toArray(OptionConfig[]::new));
		c.setSubCommands(data.getSubcommands().stream().map(SubCommandConfig::fromData).toArray(SubCommandConfig[]::new));
		c.setSubCommandGroups(data.getSubcommandGroups().stream().map(SubCommandGroupConfig::fromData).toArray(SubCommandGroupConfig[]::new));
		c.setHandler(null);
		return c;
	}

	/**
	 * Converts the current {@link CommandConfig} into a {@link SlashCommandData} object.
	 *
	 * @return The {@link SlashCommandData} object.
	 */
	public SlashCommandData toData() {
		SlashCommandData data = Commands.slash(this.name, this.description);
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CommandConfig that)) return false;
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
