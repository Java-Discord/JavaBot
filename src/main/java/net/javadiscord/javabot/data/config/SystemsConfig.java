package net.javadiscord.javabot.data.config;

import lombok.Data;

/**
 * Contains configuration settings for various systems which the bot uses, such
 * as databases or dependencies that have runtime properties.
 */
@Data
public class SystemsConfig {
	/**
	 * The Key used for bing-search-api
	 */
	public String azureSubscriptionKey = "";
	/**
	 * The token used to create the JDA Discord bot instance.
	 */
	private String jdaBotToken = "";
	/**
	 * The URL used to log in to the MongoDB instance which this bot uses.
	 */
	private String mongoDatabaseUrl = "";
	/**
	 * The number of threads to allocate to the bot's general purpose async
	 * thread pool.
	 */

	private int asyncPoolSize = 4;

	/**
	 * Configuration for the Hikari connection pool that's used for the bot's
	 * SQL data source.
	 */
	private HikariConfig hikariConfig = new HikariConfig();

	/**
	 * Configuration settings for the Hikari connection pool.
	 */
	@Data
	public static class HikariConfig {
		private String jdbcUrl = "jdbc:h2:tcp://localhost:9123/./java_bot";
		private int maximumPoolSize = 5;
	}
}
