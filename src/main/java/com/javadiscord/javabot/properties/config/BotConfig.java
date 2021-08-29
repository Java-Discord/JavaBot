package com.javadiscord.javabot.properties.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * The base container class for all the JavaBot's configuration.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class BotConfig {
	private SystemsConfig systems;
	private SlashCommandConfig slashCommand;
	private ModerationConfig moderation;
	private QOTWConfig qotw;
	private WelcomeConfig welcome;
	private StarBoardConfig starBoard;
	private JamConfig jam;

	/**
	 * The path from which the config was loaded.
	 */
	private transient Path filePath;

	/**
	 * Constructs a new empty configuration, creating a new instance of each
	 * sub-config object using its no-args constructor.
	 * @param filePath The path to the configuration file.
	 */
	public BotConfig(Path filePath) {
		this.filePath = filePath;
		for (var field : this.getClass().getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (!Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) {
				try {
					Constructor<?> constructor = field.getType().getDeclaredConstructor();
					field.set(this, constructor.newInstance());
				} catch (ReflectiveOperationException e) {
					log.error("Could not set field {} to a new instance of {}.", field.getName(), field.getType().getSimpleName());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Loads an instance of the configuration from the given path, or creates a
	 * new empty configuration file there if none exists yet.
	 * @param filePath The path to the configuration JSON file.
	 * @return The config that was loaded.
	 * @throws JsonSyntaxException if the config file's JSON is invalid.
	 * @throws UncheckedIOException if an IO error occurs.
	 */
	public static BotConfig loadOrCreate(Path filePath) {
		Gson gson = new GsonBuilder().create();
		BotConfig config;
		try (var reader = Files.newBufferedReader(filePath)) {
			config = gson.fromJson(reader, BotConfig.class);
			config.setFilePath(filePath);
			log.info("Loaded config from {}", filePath);
		} catch (JsonSyntaxException e) {
			log.error("Invalid JSON found! Please fix or remove config file " + filePath + " and restart.", e);
			throw e;
		} catch (NoSuchFileException e) {
			log.info("No config file found. Creating an empty one at {}", filePath);
			config = new BotConfig(filePath);
			config.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return config;
	}

	/**
	 * Saves this config to its file path.
	 */
	public synchronized void flush() {
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.setPrettyPrinting()
				.create();
		try (var writer = Files.newBufferedWriter(this.filePath)) {
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
