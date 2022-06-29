package net.javadiscord.javabot.data.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.data.config.guild.*;
import net.javadiscord.javabot.util.ExceptionLogger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A collection of guild-specific configuration items, each of which represents
 * a group of many individual settings.
 */
@Data
@Slf4j
public class GuildConfig {
	private transient Guild guild;
	private transient Path file;

	private HelpConfig help;
	private ModerationConfig moderation;
	private QOTWConfig qotw;
	private StatsConfig stats;
	private StarboardConfig starBoard;
	private JamConfig jam;
	private MessageCacheConfig messageCache;
	private ServerLockConfig serverLock;

	/**
	 * Constructor that initializes all Config classes.
	 *
	 * @param guild The current guild.
	 * @param file  The config file.
	 */
	public GuildConfig(Guild guild, Path file) {
		this.file = file;
		// Initialize all config items.
		this.help = new HelpConfig();
		this.moderation = new ModerationConfig();
		this.qotw = new QOTWConfig();
		this.stats = new StatsConfig();
		this.starBoard = new StarboardConfig();
		this.jam = new JamConfig();
		this.messageCache = new MessageCacheConfig();
		this.serverLock = new ServerLockConfig();
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

	private void setGuild(Guild guild) {
		this.guild = guild;
		if (this.help == null) this.help = new HelpConfig();
		this.help.setGuildConfig(this);
		if (this.moderation == null) this.moderation = new ModerationConfig();
		this.moderation.setGuildConfig(this);
		if (this.qotw == null) this.qotw = new QOTWConfig();
		this.qotw.setGuildConfig(this);
		if (this.stats == null) this.stats = new StatsConfig();
		this.stats.setGuildConfig(this);
		if (this.starBoard == null) this.starBoard = new StarboardConfig();
		this.starBoard.setGuildConfig(this);
		if (this.jam == null) this.jam = new JamConfig();
		this.jam.setGuildConfig(this);
		if (this.messageCache == null) this.messageCache = new MessageCacheConfig();
		this.messageCache.setGuildConfig(this);
		if (this.serverLock == null) this.serverLock = new ServerLockConfig();
		this.serverLock.setGuildConfig(this);
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
			ExceptionLogger.capture(e, getClass().getSimpleName());
			log.error("Could not flush config.", e);
		}
	}

	/**
	 * Attempts to resolve a configuration property value by its name, using a
	 * '.' to concatenate property names. For example, the {@link JamConfig} has
	 * a property called <code>pingRoleId</code>. We can resolve it via the
	 * full name <code>jam.pingRoleId</code>, using the <code>jam</code> field
	 * of {@link GuildConfig} followed by the <code>pingRoleId</code> field from
	 * {@link JamConfig}.
	 *
	 * @param propertyName The name of the property.
	 * @return The value of the property, if found, or null otherwise.
	 */
	@Nullable
	public Object resolve(String propertyName) throws UnknownPropertyException {
		var result = ReflectionUtils.resolveField(propertyName, this);
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
		var result = ReflectionUtils.resolveField(propertyName, this);
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