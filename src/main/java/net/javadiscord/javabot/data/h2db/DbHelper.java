package net.javadiscord.javabot.data.h2db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.SystemsConfig;
import net.javadiscord.javabot.util.ExceptionLogger;
import org.h2.tools.Server;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

/**
 * Class that provides helper methods for dealing with the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbHelper {
	@Getter
	private final DataSource dataSource;

	/**
	 * Initializes the data source that'll be used throughout the bot to access
	 * the database.
	 *
	 * @param config The bot's configuration.
	 * @return The data source.
	 * @throws IllegalStateException If an error occurs and we're unable to
	 *                               start the database.
	 */
	public static @NotNull HikariDataSource initDataSource(@NotNull BotConfig config) {
		// Determine if we need to initialize the schema, before starting up the server.
		boolean shouldInitSchema = shouldInitSchema(config.getSystems().getHikariConfig().getJdbcUrl());

		// Now that we have remembered whether we need to initialize the schema, start up the server.
		Server server;
		try {
			System.setProperty("h2.bindAddress", "127.0.0.1");
			server = Server.createTcpServer("-tcpPort", "9122", "-ifNotExists").start();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
			throw new IllegalStateException("Cannot start database server.", e);
		}
		HikariConfig hikariConfig = new HikariConfig();
		SystemsConfig.HikariConfig hikariConfigSource = config.getSystems().getHikariConfig();
		hikariConfig.setJdbcUrl(hikariConfigSource.getJdbcUrl());
		hikariConfig.setMaximumPoolSize(hikariConfigSource.getMaximumPoolSize());
		hikariConfig.setLeakDetectionThreshold(hikariConfigSource.getLeakDetectionThreshold());
		HikariDataSource ds = new HikariDataSource(hikariConfig);
		// Add a shutdown hook to close down the datasource and server when the JVM terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ds.close();
			server.stop();
		}));
		if (shouldInitSchema) {
			try {
				initializeSchema(ds);
			} catch (IOException | SQLException e) {
				ExceptionLogger.capture(e, DbHelper.class.getSimpleName());
				throw new IllegalStateException("Cannot initialize database schema.", e);
			}
		}
		return ds;
	}

	private static boolean shouldInitSchema(String jdbcUrl) {
		Pattern p = Pattern.compile("jdbc:h2:tcp://localhost:\\d+/(.*)");
		Matcher m = p.matcher(jdbcUrl);
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
		try (InputStream is = DbHelper.class.getClassLoader().getResourceAsStream("database/schema.sql")) {
			if (is == null) throw new IOException("Could not load schema.sql.");
			List<String> queries = Arrays.stream(new String(is.readAllBytes()).split(";"))
					.filter(s -> !s.isBlank()).toList();
			try (Connection c = dataSource.getConnection()) {
				for (String rawQuery : queries) {
					String query = rawQuery.lines()
							.map(s -> s.strip().stripIndent())
							.collect(Collectors.joining(""));
					try (Statement stmt = c.createStatement()) {
						stmt.executeUpdate(query);
					}
				}
			}
			log.info("Successfully initialized H2 database.");
		}
	}
}
