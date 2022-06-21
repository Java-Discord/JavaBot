package net.javadiscord.javabot.data.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The base container class for all the JavaBot's configuration.
 */
@Slf4j
public class BotConfig {
	private static final String SYSTEMS_FILE = "systems.json";

	/**
	 * The map containing guild-specific configuration settings for each guild
	 * that the bot is active in.
	 */
	private final Map<Long, GuildConfig> guilds;

	/**
	 * Global configuration settings for the bot which are not guild-specific.
	 */
	private final SystemsConfig systemsConfig;

	/**
	 * The path from which the config was loaded.
	 */
	private final Path dir;

	/**
	 * Constructs a new empty configuration.
	 *
	 * @param dir The path to the directory containing the guild configuration
	 *            files.
	 */
	public BotConfig(Path dir) {
		this.dir = dir;
		if (!(Files.exists(dir) && Files.isDirectory(dir))) {
			if (!Files.exists(dir)) {
				try {
					Files.createDirectories(dir);
				} catch (IOException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					log.error("Could not create config directory " + dir, e);
				}
			} else {
				log.error("File exists at config directory path {}", dir);
			}
		}
		this.guilds = new ConcurrentHashMap<>();
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		Path systemsFile = dir.resolve(SYSTEMS_FILE);
		if (Files.exists(systemsFile)) {
			try (var reader = Files.newBufferedReader(systemsFile)) {
				this.systemsConfig = gson.fromJson(reader, SystemsConfig.class);
				log.info("Loaded systems config from {}", systemsFile);
			} catch (JsonSyntaxException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				log.error("Invalid JSON found! Please fix or remove config file " + systemsFile + " and restart.", e);
				throw e;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			log.info("No systems config file found. Creating an empty one at {}", systemsFile);
			this.systemsConfig = new SystemsConfig();
			this.flush();
		}
	}

	/**
	 * Loads a set of guilds into the configuration at runtime.
	 *
	 * @param guilds The list of guilds to load config for.
	 */
	public void loadGuilds(@NotNull List<Guild> guilds) {
		for (Guild guild : guilds) {
			var file = dir.resolve(guild.getId() + ".json");
			var config = GuildConfig.loadOrCreate(guild, file);
			this.guilds.put(guild.getIdLong(), config);
			log.info("Loaded guild config for guild {} ({}).", guild.getName(), guild.getId());
		}
	}

	/**
	 * Adds a guild to the bot's configuration at runtime. A new, default
	 * configuration object is created, so be aware of the presence of null
	 * values.
	 *
	 * @param guild The guild to add configuration for.
	 */
	public void addGuild(@NotNull Guild guild) {
		var file = dir.resolve(guild.getId() + ".json");
		this.guilds.put(guild.getIdLong(), GuildConfig.loadOrCreate(guild, file));
		log.info("Added guild config for guild {} ({}).", guild.getName(), guild.getId());
	}

	/**
	 * Gets the configuration for a particular guild.
	 *
	 * @param guild The guild to get config for.
	 * @return The config for the given guild.
	 */
	public GuildConfig get(@Nullable Guild guild) {
		if (guild == null) return null;
		return this.guilds.computeIfAbsent(
				guild.getIdLong(),
				guildId -> new GuildConfig(guild, this.dir.resolve(guild.getId() + ".json"))
		);
	}

	public SystemsConfig getSystems() {
		return this.systemsConfig;
	}

	/**
	 * Flushes all configuration to the disk.
	 */
	public void flush() {
		Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
		Path systemsFile = this.dir.resolve(SYSTEMS_FILE);
		try (var writer = Files.newBufferedWriter(systemsFile)) {
			gson.toJson(this.systemsConfig, writer);
			writer.flush();
		} catch (IOException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			log.error("Could not save systems config.", e);
		}
		for (var config : this.guilds.values()) {
			config.flush();
		}
	}
}
