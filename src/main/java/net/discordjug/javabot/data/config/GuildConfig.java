package net.discordjug.javabot.data.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.guild.*;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Pair;
import net.dv8tion.jda.api.entities.Guild;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A collection of guild-specific configuration items, each of which represents
 * a group of many individual settings.
 */
@Data
@Slf4j
public class GuildConfig {
	private transient Guild guild;
	private transient Path file;

	private HelpConfig helpConfig;
	private ModerationConfig moderationConfig;
	private QOTWConfig qotwConfig;
	private MetricsConfig metricsConfig;
	private StarboardConfig starboardConfig;
	private MessageCacheConfig messageCacheConfig;
	private ServerLockConfig serverLockConfig;
	private List<String> blacklistedMessageExtensions;

	/**
	 * Constructor that initializes all Config classes.
	 *
	 * @param guild The current guild.
	 * @param file  The config file.
	 */
	public GuildConfig(Guild guild, Path file) {
		this.file = file;
		// Initialize all config items.
		this.helpConfig = new HelpConfig();
		this.moderationConfig = new ModerationConfig();
		this.qotwConfig = new QOTWConfig();
		this.metricsConfig = new MetricsConfig();
		this.starboardConfig = new StarboardConfig();
		this.messageCacheConfig = new MessageCacheConfig();
		this.serverLockConfig = new ServerLockConfig();
		this.blacklistedMessageExtensions = List.of("jar", "exe", "zip");
		this.setGuild(guild);
	}

	/**
	 * Loads an instance of the configuration from the given path, or creates a
	 * new empty configuration file there if none exists yet.
	 *
	 * @param guild The guild to load config for.
	 * @param file  The path to the configuration JSON file.
	 * @return The config that was loaded.
	 * @throws JsonSyntaxException  if the config file's JSON is invalid.
	 * @throws UncheckedIOException if an IO error occurs.
	 */
	public static GuildConfig loadOrCreate(Guild guild, Path file) {
		Gson gson = new GsonBuilder().create();
		GuildConfig config;
		if (Files.exists(file)) {
			try (BufferedReader reader = Files.newBufferedReader(file)) {
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

	private void setGuild(Guild guild) {
		this.guild = guild;
		if (this.helpConfig == null) this.helpConfig = new HelpConfig();
		this.helpConfig.setGuildConfig(this);
		if (this.moderationConfig == null) this.moderationConfig = new ModerationConfig();
		this.moderationConfig.setGuildConfig(this);
		if (this.qotwConfig == null) this.qotwConfig = new QOTWConfig();
		this.qotwConfig.setGuildConfig(this);
		if (this.metricsConfig == null) this.metricsConfig = new MetricsConfig();
		this.metricsConfig.setGuildConfig(this);
		if (this.starboardConfig == null) this.starboardConfig = new StarboardConfig();
		this.starboardConfig.setGuildConfig(this);
		if (this.messageCacheConfig == null) this.messageCacheConfig = new MessageCacheConfig();
		this.messageCacheConfig.setGuildConfig(this);
		if (this.serverLockConfig == null) this.serverLockConfig = new ServerLockConfig();
		this.serverLockConfig.setGuildConfig(this);
	}

	/**
	 * Saves this config to its file path.
	 */
	public synchronized void flush() {
		Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
		try (BufferedWriter writer = Files.newBufferedWriter(this.file)) {
			gson.toJson(this, writer);
			writer.flush();
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			log.error("Could not flush config.", e);
		}
	}

	/**
	 * Attempts to resolve a configuration property value by its name, using a
	 * '.' to concatenate property names. For example, the {@link ModerationConfig} has
	 * a property called <code>adminRoleId</code>. We can resolve it via the
	 * full name <code>moderation.adminRoleId</code>, using the <code>jam</code> field
	 * of {@link GuildConfig} followed by the <code>pingRoleId</code> field from
	 * {@link ModerationConfig}.
	 *
	 * @param propertyName The name of the property.
	 * @return The value of the property, if found, or null otherwise.
	 */
	@Nullable
	public Object resolve(String propertyName) throws UnknownPropertyException {
		Optional<Pair<Field, Object>> result = ReflectionUtils.resolveField(propertyName, this);
		return result.map(pair -> {
			try {
				return pair.first().get(pair.second());
			} catch (IllegalAccessException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				return null;
			}
		}).orElse(null);
	}

	/**
	 * Attempts to set a configuration property's value by its name, using '.'
	 * to concatenate property names, similar to {@link GuildConfig#resolve(String)}.
	 *
	 * @param propertyName The name of the property to set.
	 * @param value        The value to set.
	 */
	public void set(String propertyName, String value) throws UnknownPropertyException {
		Optional<Pair<Field, Object>> result = ReflectionUtils.resolveField(propertyName, this);
		result.ifPresent(pair -> {
			try {
				ReflectionUtils.set(pair.first(), pair.second(), value);
				this.flush();
			} catch (IllegalAccessException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
			}
		});
	}
}