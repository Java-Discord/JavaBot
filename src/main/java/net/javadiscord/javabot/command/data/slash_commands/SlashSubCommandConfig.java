package net.javadiscord.javabot.command.data.slash_commands;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.Arrays;

/**
 * Simple DTO for a Discord subcommand.
 */
@Data
public class SlashSubCommandConfig {
	private String name;
	private String description;
	private SlashOptionConfig[] options;

	/**
	 * Converts the given {@link SubcommandData} into a {@link SlashSubCommandConfig} object.
	 *
	 * @param data The {@link SubcommandData}.
	 * @return The {@link SlashSubCommandConfig} object.
	 */
	public static SlashSubCommandConfig fromData(SubcommandData data) {
		SlashSubCommandConfig c = new SlashSubCommandConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setOptions(data.getOptions().stream().map(SlashOptionConfig::fromData).toArray(SlashOptionConfig[]::new));
		return c;
	}

	/**
	 * Converts the current {@link SlashSubCommandConfig} into a {@link SubcommandData} object.
	 *
	 * @return The {@link SubcommandData} object.
	 */
	public SubcommandData toData() {
		SubcommandData data = new SubcommandData(this.name, this.description);
		if (this.options != null) {
			for (SlashOptionConfig oc : this.options) {
				data.addOptions(oc.toData());
			}
		}
		return data;
	}

	@Override
	public String toString() {
		return "SubCommandConfig{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", options=" + Arrays.toString(options) +
				'}';
	}
}
