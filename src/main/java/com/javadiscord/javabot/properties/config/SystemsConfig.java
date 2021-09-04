package com.javadiscord.javabot.properties.config;

import lombok.Data;

/**
 * Contains configuration settings for various systems which the bot uses, such
 * as databases or dependencies that have runtime properties.
 */
@Data
public class SystemsConfig {
	/**
	 * The token used to create the JDA Discord bot instance.
	 */
	private String jdaBotToken = "";

	/**
	 * The name of the local H2 database file (excluding file extension).
	 */
	private String h2DatabaseFileName = "java_bot";

	/**
	 * The URL used to log in to the MongoDB instance which this bot uses.
	 */
	private String mongoDatabaseUrl = "";

	/**
	 * The number of threads to allocate to the bot's general purpose async
	 * thread pool.
	 */
	private int asyncPoolSize = 4;
}
