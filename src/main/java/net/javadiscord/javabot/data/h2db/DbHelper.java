package net.javadiscord.javabot.data.h2db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.data.config.BotConfig;
import org.h2.tools.Server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Class that provides helper methods for dealing with the database.
 */
@Slf4j
public class DbHelper {
	private DbHelper() {}

	/**
	 * Initializes the data source that'll be used throughout the bot to access
	 * the database.
	 * @param config The bot's configuration.
	 * @return The data source.
	 * @throws IllegalStateException If an error occurs, and we're unable to
	 * start the database.
	 */
	public static HikariDataSource initDataSource(BotConfig config) {
		// Determine if we need to initialize the schema, before starting up the server.
		boolean shouldInitSchema = shouldInitSchema(config.getSystems().getHikariConfig().getJdbcUrl());

		// Now that we have remembered whether we need to initialize the schema, start up the server.
		Server server;
		try {
			server = Server.createTcpServer("-tcpPort", "9123", "-ifNotExists").start();
		} catch (SQLException e) {
			throw new IllegalStateException("Cannot start database server.", e);
		}
		var hikariConfig = new HikariConfig();
		var hikariConfigSource = config.getSystems().getHikariConfig();
		hikariConfig.setJdbcUrl(hikariConfigSource.getJdbcUrl());
		hikariConfig.setMaximumPoolSize(hikariConfigSource.getMaximumPoolSize());
		var ds = new HikariDataSource(hikariConfig);
		// Add a shutdown hook to close down the datasource and server when the JVM terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ds.close();
			server.stop();
		}));
		if (shouldInitSchema) {
			try {
				initializeSchema(ds);
			} catch (IOException | SQLException e) {
				e.printStackTrace();
				throw new IllegalStateException("Cannot initialize database schema.", e);
			}
		}
		return ds;
	}

	private static boolean shouldInitSchema(String jdbcUrl) {
		var p = Pattern.compile("jdbc:h2:tcp://localhost:\\d+/(.*)");
		var m = p.matcher(jdbcUrl);
		boolean shouldInitSchema = false;
		if (m.find()) {
			String dbFilePath = m.group(1) + ".mv.db";
			if (Files.notExists(Path.of(dbFilePath))) {
				log.info("Database file doesn't exist yet. Initializing schema.");
				shouldInitSchema = true;
			}
		} else {
			throw new IllegalArgumentException("Invalid JDBC URL. Should point to a file.");
		}
		return shouldInitSchema;
	}

	private static void initializeSchema(HikariDataSource dataSource) throws IOException, SQLException {
		InputStream is = DbHelper.class.getResourceAsStream("/db/schema.sql");
		if (is == null) throw new IOException("Could not load schema.sql.");
		var queries = Arrays.stream(new String(is.readAllBytes()).split(";"))
				.filter(s -> !s.isBlank()).toList();
		try (var c = dataSource.getConnection()) {
			for (var query : queries) {
				var stmt = c.createStatement();
				stmt.executeUpdate(query);
				stmt.close();
			}
		}
		log.info("Successfully initialized H2 database.");
	}
}
