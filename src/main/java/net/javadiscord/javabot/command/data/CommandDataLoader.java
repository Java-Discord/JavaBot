package net.javadiscord.javabot.command.data;

import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.command.data.context_commands.ContextCommandConfig;
import net.javadiscord.javabot.command.data.slash_commands.SlashCommandConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple helper class that loads an array of * instances
 * from the YAML files.
 */
@Slf4j
public final class CommandDataLoader {
	private CommandDataLoader() {
	}

	/**
	 * Loads an array of {@link SlashCommandConfig} from the given set of classpath
	 * resources.
	 *
	 * @param resources The list of resources to read from.
	 * @return An array of slash command data objects.
	 */
	public static SlashCommandConfig[] loadSlashCommandConfig(String... resources) {
		Yaml yaml = new Yaml();
		Set<SlashCommandConfig> commands = new HashSet<>();
		for (var resource : resources) {
			InputStream is = CommandDataLoader.class.getClassLoader().getResourceAsStream(resource);
			if (is == null) {
				System.err.println("Could not load commands from resource: " + resource);
				continue;
			}
			SlashCommandConfig[] cs = yaml.loadAs(is, SlashCommandConfig[].class);
			if (cs != null) {
				for (var newCommand : cs) {
					if (commands.contains(newCommand)) {
						log.warn("Found duplicate command {} in file {}. Already loaded a command with this name; this one will be ignored.", newCommand.getName(), resource);
					}
					commands.add(newCommand);
				}
			}
		}
		return commands.toArray(new SlashCommandConfig[0]);
	}

	/**
	 * Loads an array of {@link ContextCommandConfig} from the given set of classpath
	 * resources.
	 *
	 * @param resources The list of resources to read from.
	 * @return An array of slash command data objects.
	 */
	public static ContextCommandConfig[] loadContextCommandConfig(String... resources) {
		Yaml yaml = new Yaml();
		Set<ContextCommandConfig> commands = new HashSet<>();
		for (var resource : resources) {
			InputStream is = CommandDataLoader.class.getClassLoader().getResourceAsStream(resource);
			if (is == null) {
				System.err.println("Could not load commands from resource: " + resource);
				continue;
			}
			ContextCommandConfig[] cs = yaml.loadAs(is, ContextCommandConfig[].class);
			if (cs != null) {
				for (var newCommand : cs) {
					if (commands.contains(newCommand)) {
						log.warn("Found duplicate command {} in file {}. Already loaded a command with this name; this one will be ignored.", newCommand.getName(), resource);
					}
					commands.add(newCommand);
				}
			}
		}
		return commands.toArray(new ContextCommandConfig[0]);
	}
}
