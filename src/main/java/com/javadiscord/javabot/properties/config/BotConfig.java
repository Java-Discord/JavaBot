package com.javadiscord.javabot.properties.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The base container class for all the JavaBot's configuration.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BotConfig {
	private ModerationConfig moderationConfig;
	private QOTWConfig qotwConfig;
	private WelcomeConfig welcomeConfig;
	private StarBoardConfig starBoardConfig;
	private JamConfig jamConfig;


	private transient Path filePath;

	public BotConfig(Path filePath) {
		this.filePath = filePath;
		this.moderationConfig = new ModerationConfig();
		this.qotwConfig = new QOTWConfig();
		this.welcomeConfig = new WelcomeConfig();
		this.starBoardConfig = new StarBoardConfig();
		this.jamConfig = new JamConfig();
	}

	/**
	 * Loads an instance of the configuration from the given path.
	 * @param filePath The path to the configuration JSON file.
	 * @return The config that was loaded.
	 * @throws IOException If the file cannot be found or read.
	 */
	public static synchronized BotConfig load(Path filePath) throws IOException {
		Gson gson = new Gson();
		var config = gson.fromJson(Files.newBufferedReader(filePath), BotConfig.class);
		if (config == null) throw new IOException("Config file is empty.");
		config.setFilePath(filePath);
		return config;
	}

	/**
	 * Saves this config to its file path.
	 * @throws IOException If the file cannot be written to.
	 */
	public synchronized void save() throws IOException {
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.setPrettyPrinting()
				.create();
		gson.toJson(this, Files.newBufferedWriter(this.filePath));
		System.out.println("Config Json: " + gson.toJson(this));
	}
}
