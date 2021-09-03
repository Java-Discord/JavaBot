package com.javadiscord.javabot.properties.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.javadiscord.javabot.properties.config.guild.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * A collection of guild-specific configuration items, each of which represents
 * a group of many individual settings.
 */
@Data
@Slf4j
public class GuildConfig {
	private transient Guild guild;
	private transient Path file;

	private SlashCommandConfig slashCommand;
	private ModerationConfig moderation;
	private QOTWConfig qotw;
	private WelcomeConfig welcome;
	private StatsConfig stats;
	private StarBoardConfig starBoard;
	private JamConfig jam;

	public GuildConfig(Guild guild, Path file) {
		this.file = file;
		// Initialize all config items.
		this.slashCommand = new SlashCommandConfig();
		this.moderation = new ModerationConfig();
		this.qotw = new QOTWConfig();
		this.welcome = new WelcomeConfig();
		this.stats = new StatsConfig();
		this.starBoard = new StarBoardConfig();
		this.jam = new JamConfig();
		this.setGuild(guild);
	}

	private void setGuild(Guild guild) {
		this.guild = guild;
		this.slashCommand.setGuild(guild);
		this.moderation.setGuild(guild);
		this.qotw.setGuild(guild);
		this.welcome.setGuild(guild);
		this.stats.setGuild(guild);
		this.starBoard.setGuild(guild);
		this.jam.setGuild(guild);
	}

	/**
	 * Loads an instance of the configuration from the given path, or creates a
	 * new empty configuration file there if none exists yet.
	 * @param guild The guild to load config for.
	 * @param file The path to the configuration JSON file.
	 * @return The config that was loaded.
	 * @throws JsonSyntaxException if the config file's JSON is invalid.
	 * @throws UncheckedIOException if an IO error occurs.
	 */
	public static GuildConfig loadOrCreate(Guild guild, Path file) {
		Gson gson = new GsonBuilder().create();
		GuildConfig config;
		if (Files.exists(file)) {
			try (var reader = Files.newBufferedReader(file)) {
				config = gson.fromJson(reader, GuildConfig.class);
				config.setFile(file);
				config.setGuild(guild);
				log.info("Loaded config from {}", file);
			} catch (JsonSyntaxException e) {
				log.error("Invalid JSON found! Please fix or remove config file " + file + " and restart.", e);
				throw e;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			log.info("No config file found. Creating an empty one at {}", file);
			config = new GuildConfig(guild, file);
			config.flush();
		}

		return config;
	}

	/**
	 * Saves this config to its file path.
	 */
	public synchronized void flush() {
		Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
		try (var writer = Files.newBufferedWriter(this.file)) {
			gson.toJson(this, writer);
			writer.flush();
		} catch (IOException e) {
			log.error("Could not flush config.", e);
		}
	}

	/**
	 * Attempts to resolve a configuration property value by its name, using a
	 * '.' to concatenate property names. For example, the {@link JamConfig} has
	 * a property called <code>pingRoleId</code>. We can resolve it via the
	 * full name <code>jamConfig.pingRoleId</code>.
	 * @param propertyName The name of the property.
	 * @param <T> The type of the property.
	 * @return The value of the property, if found, or null otherwise.
	 */
	@Nullable
	public <T> T resolve(String propertyName) {
		return this.resolveRecursive(propertyName.split("\\."), this);
	}

	/**
	 * Recursively resolves a property based on a sequential array of field
	 * names to traverse. If there's only one field name present, we will try
	 * to find the field in the given parent object and return its value. If
	 * there are more field names, however, get the value of the first field
	 * from the parent, and recursively check the fields of that value.
	 * @param fieldNames The array of field names.
	 * @param parent The object in which to look for root-level fields.
	 * @param <T> The expected return type.
	 * @return The object which was resolved, or null otherwise.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	private <T> T resolveRecursive(String[] fieldNames, Object parent) {
		if (fieldNames.length == 0) return null;
		try {
			Field field = parent.getClass().getDeclaredField(fieldNames[0]);
			field.setAccessible(true);
			Object value = field.get(parent);
			if (fieldNames.length == 1) {
				return (T) value;
			} else if (value != null) {
				return resolveRecursive(Arrays.copyOfRange(fieldNames, 1, fieldNames.length), value);
			} else {
				return null;
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.warn("Reflection error occurred while resolving property " + Arrays.toString(fieldNames) + " of object of type " + parent.getClass().getSimpleName(), e);
			return null;
		}
	}
}
